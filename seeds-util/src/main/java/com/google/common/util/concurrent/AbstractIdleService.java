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

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Throwables;

import java.util.concurrent.Executor;

/**
 * Base class for services that do not need a thread while "running"
 * but may need one during startup and shutdown. Subclasses can
 * implement {@link #startUp} and {@link #shutDown} methods, each
 * which run in a executor which by default uses a separate thread
 * for each method.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
public abstract class AbstractIdleService implements Service {

  /* use AbstractService for state management */
  private final Service delegate = new AbstractService() {
    @Override protected final void doStart() {
      executor().execute(new Runnable() {
        @Override public void run() {
          try {
            startUp();
            notifyStarted();
          } catch (Throwable t) {
            notifyFailed(t);
            throw Throwables.propagate(t);
          }
        }
      });
    }

    @Override protected final void doStop() {
      executor().execute(new Runnable() {
        @Override public void run() {
          try {
            shutDown();
            notifyStopped();
          } catch (Throwable t) {
            notifyFailed(t);
            throw Throwables.propagate(t);
          }
        }
      });
    }
  };

  /** Constructor for use by subclasses. */
  protected AbstractIdleService() {}

  /** Start the service. */
  protected abstract void startUp() throws Exception;

  /** Stop the service. */
  protected abstract void shutDown() throws Exception;

  /**
   * Returns the {@link Executor} that will be used to run this service.
   * Subclasses may override this method to use a custom {@link Executor}, which
   * may configure its worker thread with a specific name, thread group or
   * priority. The returned executor's {@link Executor#execute(Runnable)
   * execute()} method is called when this service is started and stopped,
   * and should return promptly.
   */
  protected Executor executor() {
    final State state = state();
    return new Executor() {
      @Override
      public void execute(Runnable command) {
        MoreExecutors.newThread(serviceName() + " " + state, command).start();
      }
    };
  }

  @Override public String toString() {
    return serviceName() + " [" + state() + "]";
  }

  // We override instead of using ForwardingService so that these can be final.

  @Override public final ListenableFuture<State> start() {
    return delegate.start();
  }

  @Override public final State startAndWait() {
    return delegate.startAndWait();
  }

  @Override public final boolean isRunning() {
    return delegate.isRunning();
  }

  @Override public final State state() {
    return delegate.state();
  }

  @Override public final ListenableFuture<State> stop() {
    return delegate.stop();
  }

  @Override public final State stopAndWait() {
    return delegate.stopAndWait();
  }
  
  /**
   * @since 13.0
   */
  @Override public final void addListener(Listener listener, Executor executor) {
    delegate.addListener(listener, executor);
  }
  
  /**
   * @since 14.0
   */
  @Override public final Throwable failureCause() {
    return delegate.failureCause();
  }
  
  /**
   * Returns the name of this service. {@link AbstractIdleService} may include the name in debugging
   * output.
   *
   * @since 14.0
   */
  protected String serviceName() {
    return getClass().getSimpleName();
  }
}
