/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tribe7.common.util.concurrent;

import static net.tribe7.common.base.Preconditions.checkArgument;
import static net.tribe7.common.base.Preconditions.checkNotNull;
import static net.tribe7.common.base.Preconditions.checkState;
import static net.tribe7.common.util.concurrent.Service.State.FAILED;
import static net.tribe7.common.util.concurrent.Service.State.NEW;
import static net.tribe7.common.util.concurrent.Service.State.RUNNING;
import static net.tribe7.common.util.concurrent.Service.State.STARTING;
import static net.tribe7.common.util.concurrent.Service.State.STOPPING;
import static net.tribe7.common.util.concurrent.Service.State.TERMINATED;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;

import net.tribe7.common.annotations.Beta;
import net.tribe7.common.collect.Lists;
import net.tribe7.common.util.concurrent.Monitor.Guard;
import net.tribe7.common.util.concurrent.Service.State;

/**
 * Base class for implementing services that can handle {@link #doStart} and {@link #doStop}
 * requests, responding to them with {@link #notifyStarted()} and {@link #notifyStopped()}
 * callbacks. Its subclasses must manage threads manually; consider
 * {@link AbstractExecutionThreadService} if you need only a single execution thread.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 1.0
 */
@Beta
public abstract class AbstractService implements Service {
  private final Monitor monitor = new Monitor();

  private final Transition startup = new Transition();
  private final Transition shutdown = new Transition();

  private final Guard isStartable = new Guard(monitor) {
    @Override public boolean isSatisfied() {
      return state() == NEW;
    }
  };

  private final Guard isStoppable = new Guard(monitor) {
    @Override public boolean isSatisfied() {
      return state().compareTo(RUNNING) <= 0;
    }
  };

  private final Guard hasReachedRunning = new Guard(monitor) {
    @Override public boolean isSatisfied() {
      return state().compareTo(RUNNING) >= 0;
    }
  };

  private final Guard isStopped = new Guard(monitor) {
    @Override public boolean isSatisfied() {
      return state().isTerminal();
    }
  };

  /**
   * The listeners to notify during a state transition.
   */
  @GuardedBy("monitor")
  private final List<ListenerExecutorPair> listeners = Lists.newArrayList();

  /**
   * The queue of listeners that are waiting to be executed.
   *
   * <p>Enqueue operations should be protected by {@link #monitor} while calling
   * {@link ExecutionQueue#execute()} should not be protected.
   */
  private final ExecutionQueue queuedListeners = new ExecutionQueue();

  /**
   * The current state of the service.  This should be written with the lock held but can be read
   * without it because it is an immutable object in a volatile field.  This is desirable so that
   * methods like {@link #state}, {@link #failureCause} and notably {@link #toString} can be run
   * without grabbing the lock.
   *
   * <p>To update this field correctly the lock must be held to guarantee that the state is
   * consistent.
   */
  @GuardedBy("monitor")
  private volatile StateSnapshot snapshot = new StateSnapshot(NEW);

  /** Constructor for use by subclasses. */
  protected AbstractService() {
    // Add a listener to update the futures. This needs to be added first so that it is executed
    // before the other listeners. This way the other listeners can access the completed futures.
    addListener(
        new Listener() {
          @Override public void running() {
            startup.set(RUNNING);
          }

          @Override public void stopping(State from) {
            if (from == STARTING) {
              startup.set(STOPPING);
            }
          }

          @Override public void terminated(State from) {
            if (from == NEW) {
              startup.set(TERMINATED);
            }
            shutdown.set(TERMINATED);
          }

          @Override public void failed(State from, Throwable failure) {
            switch (from) {
              case STARTING:
                startup.setException(failure);
                shutdown.setException(new Exception("Service failed to start.", failure));
                break;
              case RUNNING:
                shutdown.setException(new Exception("Service failed while running", failure));
                break;
              case STOPPING:
                shutdown.setException(failure);
                break;
              case TERMINATED:  /* fall-through */
              case FAILED:  /* fall-through */
              case NEW:  /* fall-through */
              default:
                throw new AssertionError("Unexpected from state: " + from);
            }
          }
        },
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * This method is called by {@link #start} to initiate service startup. The invocation of this
   * method should cause a call to {@link #notifyStarted()}, either during this method's run, or
   * after it has returned. If startup fails, the invocation should cause a call to
   * {@link #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service startup, even when {@link #start} is called
   * multiple times.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation of this method should
   * cause a call to {@link #notifyStopped()}, either during this method's run, or after it has
   * returned. If shutdown fails, the invocation should cause a call to
   * {@link #notifyFailed(Throwable)} instead.
   *
   * <p> This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service shutdown, even when {@link #stop} is called
   * multiple times.
   */
  protected abstract void doStop();

  @Override public final Service startAsync() {
    if (monitor.enterIf(isStartable)) {
      try {
        snapshot = new StateSnapshot(STARTING);
        starting();
        doStart();
       // TODO(user): justify why we are catching Throwable and not RuntimeException
      } catch (Throwable startupFailure) {
        notifyFailed(startupFailure);
      } finally {
        monitor.leave();
        executeListeners();
      }
    } else {
      throw new IllegalStateException("Service " + this + " has already been started");
    }
    return this;
  }

  @Deprecated
  @Override
  public final ListenableFuture<State> start() {
    if (monitor.enterIf(isStartable)) {
      try {
        snapshot = new StateSnapshot(STARTING);
        starting();
        doStart();
      } catch (Throwable startupFailure) {
        notifyFailed(startupFailure);
      } finally {
        monitor.leave();
        executeListeners();
      }
    }
    return startup;
  }

  @Override public final Service stopAsync() {
    stop();
    return this;
  }

  @Deprecated
  @Override
  public final ListenableFuture<State> stop() {
    if (monitor.enterIf(isStoppable)) {
      try {
        State previous = state();
        switch (previous) {
          case NEW:
            snapshot = new StateSnapshot(TERMINATED);
            terminated(NEW);
            break;
          case STARTING:
            snapshot = new StateSnapshot(STARTING, true, null);
            stopping(STARTING);
            break;
          case RUNNING:
            snapshot = new StateSnapshot(STOPPING);
            stopping(RUNNING);
            doStop();
            break;
          case STOPPING:
          case TERMINATED:
          case FAILED:
            // These cases are impossible due to the if statement above.
            throw new AssertionError("isStoppable is incorrectly implemented, saw: " + previous);
          default:
            throw new AssertionError("Unexpected state: " + previous);
        }
        // TODO(user): justify why we are catching Throwable and not RuntimeException.  Also, we
        // may inadvertently catch our AssertionErrors.
      } catch (Throwable shutdownFailure) {
        notifyFailed(shutdownFailure);
      } finally {
        monitor.leave();
        executeListeners();
      }
    }
    return shutdown;
  }

  @Deprecated
  @Override
  public State startAndWait() {
    return Futures.getUnchecked(start());
  }

  @Deprecated
  @Override
  public State stopAndWait() {
    return Futures.getUnchecked(stop());
  }

  @Override public final void awaitRunning() {
    monitor.enterWhenUninterruptibly(hasReachedRunning);
    try {
      checkCurrentState(RUNNING);
    } finally {
      monitor.leave();
    }
  }

  @Override public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(hasReachedRunning, timeout, unit)) {
      try {
        checkCurrentState(RUNNING);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard.  I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException("Timed out waiting for " + this + " to reach the RUNNING state. "
          + "Current state: " + state());
    }
  }

  @Override public final void awaitTerminated() {
    monitor.enterWhenUninterruptibly(isStopped);
    try {
      checkCurrentState(TERMINATED);
    } finally {
      monitor.leave();
    }
  }

  @Override public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(isStopped, timeout, unit)) {
      try {
        State state = state();
        checkCurrentState(TERMINATED);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard.  I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException("Timed out waiting for " + this + " to reach a terminal state. "
          + "Current state: " + state());
    }
  }

  /** Checks that the current state is equal to the expected state. */
  @GuardedBy("monitor")
  private void checkCurrentState(State expected) {
    State actual = state();
    if (actual != expected) {
      if (actual == FAILED) {
        // Handle this specially so that we can include the failureCause, if there is one.
        throw new IllegalStateException("Expected the service to be " + expected
            + ", but the service has FAILED", failureCause());
      }
      throw new IllegalStateException("Expected the service to be " + expected + ", but was "
          + actual);
    }
  }

  /**
   * Implementing classes should invoke this method once their service has started. It will cause
   * the service to transition from {@link State#STARTING} to {@link State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not {@link State#STARTING}.
   */
  protected final void notifyStarted() {
    monitor.enter();
    try {
      // We have to examine the internal state of the snapshot here to properly handle the stop
      // while starting case.
      if (snapshot.state != STARTING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStarted() when the service is " + snapshot.state);
        notifyFailed(failure);
        throw failure;
      }

      if (snapshot.shutdownWhenStartupFinishes) {
        snapshot = new StateSnapshot(STOPPING);
        // We don't call listeners here because we already did that when we set the
        // shutdownWhenStartupFinishes flag.
        doStop();
      } else {
        snapshot = new StateSnapshot(RUNNING);
        running();
      }
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  /**
   * Implementing classes should invoke this method once their service has stopped. It will cause
   * the service to transition from {@link State#STOPPING} to {@link State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is neither {@link State#STOPPING} nor
   *         {@link State#RUNNING}.
   */
  protected final void notifyStopped() {
    monitor.enter();
    try {
      // We check the internal state of the snapshot instead of state() directly so we don't allow
      // notifyStopped() to be called while STARTING, even if stop() has already been called.
      State previous = snapshot.state;
      if (previous != STOPPING && previous != RUNNING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStopped() when the service is " + previous);
        notifyFailed(failure);
        throw failure;
      }
      snapshot = new StateSnapshot(TERMINATED);
      terminated(previous);
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  /**
   * Invoke this method to transition the service to the {@link State#FAILED}. The service will
   * <b>not be stopped</b> if it is running. Invoke this method when a service has failed critically
   * or otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    checkNotNull(cause);

    monitor.enter();
    try {
      State previous = state();
      switch (previous) {
        case NEW:
        case TERMINATED:
          throw new IllegalStateException("Failed while in state:" + previous, cause);
        case RUNNING:
        case STARTING:
        case STOPPING:
          snapshot = new StateSnapshot(FAILED, false, cause);
          failed(previous, cause);
          break;
        case FAILED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unexpected state: " + previous);
      }
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  @Override
  public final boolean isRunning() {
    return state() == RUNNING;
  }

  @Override
  public final State state() {
    return snapshot.externalState();
  }

  /**
   * @since 14.0
   */
  @Override
  public final Throwable failureCause() {
    return snapshot.failureCause();
  }

  /**
   * @since 13.0
   */
  @Override
  public final void addListener(Listener listener, Executor executor) {
    checkNotNull(listener, "listener");
    checkNotNull(executor, "executor");
    monitor.enter();
    try {
      State currentState = state();
      if (currentState != TERMINATED && currentState != FAILED) {
        listeners.add(new ListenerExecutorPair(listener, executor));
      }
    } finally {
      monitor.leave();
    }
  }

  @Override public String toString() {
    return getClass().getSimpleName() + " [" + state() + "]";
  }

  /**
   * A change from one service state to another, plus the result of the change.
   */
  private class Transition extends AbstractFuture<State> {
    @Override
    public State get(long timeout, TimeUnit unit)
        throws InterruptedException, TimeoutException, ExecutionException {
      try {
        return super.get(timeout, unit);
      } catch (TimeoutException e) {
        throw new TimeoutException(AbstractService.this.toString());
      }
    }
  }

  /**
   * Attempts to execute all the listeners in {@link #queuedListeners} while not holding the
   * {@link #monitor}.
   */
  private void executeListeners() {
    if (!monitor.isOccupiedByCurrentThread()) {
      queuedListeners.execute();
    }
  }

  @GuardedBy("monitor")
  private void starting() {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.listener.starting();
        }
      }, pair.executor);
    }
  }

  @GuardedBy("monitor")
  private void running() {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.listener.running();
        }
      }, pair.executor);
    }
  }

  @GuardedBy("monitor")
  private void stopping(final State from) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.listener.stopping(from);
        }
      }, pair.executor);
    }
  }

  @GuardedBy("monitor")
  private void terminated(final State from) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.listener.terminated(from);
        }
      }, pair.executor);
    }
    // There are no more state transitions so we can clear this out.
    listeners.clear();
  }

  @GuardedBy("monitor")
  private void failed(final State from, final Throwable cause) {
    for (final ListenerExecutorPair pair : listeners) {
      queuedListeners.add(new Runnable() {
        @Override public void run() {
          pair.listener.failed(from, cause);
        }
      }, pair.executor);
    }
    // There are no more state transitions so we can clear this out.
    listeners.clear();
  }

  /** A simple holder for a listener and its executor. */
  private static class ListenerExecutorPair {
    final Listener listener;
    final Executor executor;

    ListenerExecutorPair(Listener listener, Executor executor) {
      this.listener = listener;
      this.executor = executor;
    }
  }

  /**
   * An immutable snapshot of the current state of the service. This class represents a consistent
   * snapshot of the state and therefore it can be used to answer simple queries without needing to
   * grab a lock.
   */
  @Immutable
  private static final class StateSnapshot {
    /**
     * The internal state, which equals external state unless
     * shutdownWhenStartupFinishes is true.
     */
    final State state;

    /**
     * If true, the user requested a shutdown while the service was still starting
     * up.
     */
    final boolean shutdownWhenStartupFinishes;

    /**
     * The exception that caused this service to fail.  This will be {@code null}
     * unless the service has failed.
     */
    @Nullable
    final Throwable failure;

    StateSnapshot(State internalState) {
      this(internalState, false, null);
    }

    StateSnapshot(
        State internalState, boolean shutdownWhenStartupFinishes, @Nullable Throwable failure) {
      checkArgument(!shutdownWhenStartupFinishes || internalState == STARTING,
          "shudownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.",
          internalState);
      checkArgument(!(failure != null ^ internalState == FAILED),
          "A failure cause should be set if and only if the state is failed.  Got %s and %s "
          + "instead.", internalState, failure);
      this.state = internalState;
      this.shutdownWhenStartupFinishes = shutdownWhenStartupFinishes;
      this.failure = failure;
    }

    /** @see Service#state() */
    State externalState() {
      if (shutdownWhenStartupFinishes && state == STARTING) {
        return STOPPING;
      } else {
        return state;
      }
    }

    /** @see Service#failureCause() */
    Throwable failureCause() {
      checkState(state == FAILED,
          "failureCause() is only valid if the service has failed, service is %s", state);
      return failure;
    }
  }
}
