/*
 * Copyright (C) 2012 The Guava Authors
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

package net.tribe7.common.collect;

import static net.tribe7.common.base.Preconditions.checkNotNull;
import static net.tribe7.common.base.Predicates.compose;
import static net.tribe7.common.base.Predicates.in;
import static net.tribe7.common.base.Predicates.not;

import net.tribe7.common.annotations.GwtCompatible;
import net.tribe7.common.base.Objects;
import net.tribe7.common.base.Predicate;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Implementation of {@link Multimaps#filterEntries(Multimap, Predicate)}.
 * 
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
class FilteredEntryMultimap<K, V> extends FilteredMultimap<K, V> {
  final Predicate<? super Entry<K, V>> predicate;

  FilteredEntryMultimap(Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
    super(unfiltered);
    this.predicate = checkNotNull(predicate);
  }

  @Override
  Predicate<? super Entry<K, V>> entryPredicate() {
    return predicate;
  }

  @Override
  public int size() {
    return entries().size();
  }

  private boolean satisfies(K key, V value) {
    return predicate.apply(Maps.immutableEntry(key, value));
  }
  

  final class ValuePredicate implements Predicate<V> {
    private final K key;

    ValuePredicate(K key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nullable V value) {
      return satisfies(key, value);
    }
  }

  static <E> Collection<E> filterCollection(
      Collection<E> collection, Predicate<? super E> predicate) {
    if (collection instanceof Set) {
      return Sets.filter((Set<E>) collection, predicate);
    } else {
      return Collections2.filter(collection, predicate);
    }
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return asMap().get(key) != null;
  }

  @Override
  public Collection<V> removeAll(@Nullable Object key) {
    return Objects.firstNonNull(asMap().remove(key), unmodifiableEmptyCollection());
  }

  Collection<V> unmodifiableEmptyCollection() {
    // These return false, rather than throwing a UOE, on remove calls.
    return (unfiltered instanceof SetMultimap) 
        ? Collections.<V>emptySet() 
        : Collections.<V>emptyList();
  }

  @Override
  public void clear() {
    entries().clear();
  }

  @Override
  public Collection<V> get(final K key) {
    return filterCollection(unfiltered.get(key), new ValuePredicate(key));
  }

  @Override
  Collection<Entry<K, V>> createEntries() {
    return filterCollection(unfiltered.entries(), predicate);
  }

  @Override
  Iterator<Entry<K, V>> entryIterator() {
    throw new AssertionError("should never be called");
  }

  @Override
  Map<K, Collection<V>> createAsMap() {
    return new AsMap();
  }
  
  @Override
  public Set<K> keySet() {
    return asMap().keySet();
  }
  
  boolean removeIf(Predicate<? super Entry<K, Collection<V>>> predicate) {
    Iterator<Entry<K, Collection<V>>> entryIterator = unfiltered.asMap().entrySet().iterator();
    boolean changed = false;
    while (entryIterator.hasNext()) {
      Entry<K, Collection<V>> entry = entryIterator.next();
      K key = entry.getKey();
      Collection<V> collection = filterCollection(entry.getValue(), new ValuePredicate(key));
      if (!collection.isEmpty() && predicate.apply(Maps.immutableEntry(key, collection))) {
        if (collection.size() == entry.getValue().size()) {
          entryIterator.remove();
        } else {
          collection.clear();
        }
        changed = true;
      }
    }
    return changed;
  }
  
  class AsMap extends AbstractMap<K, Collection<V>> {
    @Override
    public boolean containsKey(@Nullable Object key) {
      return get(key) != null;
    }

    @Override
    public void clear() {
      FilteredEntryMultimap.this.clear();
    }

    @Override
    public Collection<V> get(@Nullable Object key) {
      Collection<V> result = unfiltered.asMap().get(key);
      if (result == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      result = filterCollection(result, new ValuePredicate(k));
      return result.isEmpty() ? null : result;
    }
    
    @Override
    public Collection<V> remove(@Nullable Object key) {
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // it's definitely equal to a K
      K k = (K) key;
      List<V> result = Lists.newArrayList();
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          itr.remove();
          result.add(v);
        }
      }
      if (result.isEmpty()) {
        return null;
      } else if (unfiltered instanceof SetMultimap) {
        return Collections.unmodifiableSet(Sets.newLinkedHashSet(result));
      } else {
        return Collections.unmodifiableList(result);
      }
    }
    
    private Set<K> keySet;
    
    @Override
    public Set<K> keySet() {
      Set<K> result = keySet;
      if (result == null) {
        return keySet = new Maps.KeySet<K, Collection<V>>() {
          @Override
          Map<K, Collection<V>> map() {
            return AsMap.this;
          }
          
          @Override
          public boolean removeAll(Collection<?> c) {
            return removeIf(compose(in(c), Maps.<K>keyFunction()));
          }
          
          @Override
          public boolean retainAll(Collection<?> c) {
            return removeIf(compose(not(in(c)), Maps.<K>keyFunction()));
          }
          
          @Override
          public boolean remove(@Nullable Object o) {
            return AsMap.this.remove(o) != null;
          }
        };
      }
      return result;
    }

    @Override
    public Set<Entry<K, Collection<V>>> entrySet() {
      return new Maps.EntrySet<K, Collection<V>>() {
        @Override
        Map<K, Collection<V>> map() {
          return AsMap.this;
        }

        @Override
        public Iterator<Entry<K, Collection<V>>> iterator() {
          return new AbstractIterator<Entry<K, Collection<V>>>() {
            final Iterator<Entry<K, Collection<V>>> backingIterator 
                = unfiltered.asMap().entrySet().iterator();

            @Override
            protected Entry<K, Collection<V>> computeNext() {
              while (backingIterator.hasNext()) {
                Entry<K, Collection<V>> entry = backingIterator.next();
                K key = entry.getKey();
                Collection<V> collection 
                    = filterCollection(entry.getValue(), new ValuePredicate(key));
                if (!collection.isEmpty()) {
                  return Maps.immutableEntry(key, collection);
                }
              }
              return endOfData();
            }
          };
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeIf(in(c));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeIf(not(in(c)));
        }
        
        @Override
        public int size() {
          return Iterators.size(iterator());
        }
      };
    }
    
    @Override
    public Collection<Collection<V>> values() {
      return new Maps.Values<K, Collection<V>>() {
        @Override
        Map<K, Collection<V>> map() {
          return AsMap.this;
        }

        @Override
        public boolean remove(@Nullable Object o) {
          if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            Iterator<Entry<K, Collection<V>>> entryIterator 
                = unfiltered.asMap().entrySet().iterator();
            while (entryIterator.hasNext()) {
              Entry<K, Collection<V>> entry = entryIterator.next();
              K key = entry.getKey();
              Collection<V> collection 
                  = filterCollection(entry.getValue(), new ValuePredicate(key));
              if (!collection.isEmpty() && c.equals(collection)) {
                if (collection.size() == entry.getValue().size()) {
                  entryIterator.remove();
                } else {
                  collection.clear();
                }
                return true;
              }
            }
          }
          return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeIf(compose(in(c), Maps.<Collection<V>>valueFunction()));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeIf(compose(not(in(c)), Maps.<Collection<V>>valueFunction()));
        }
      };
    }
  }
  
  @Override
  Multiset<K> createKeys() {
    return new Keys();
  }
  
  class Keys extends Multimaps.Keys<K, V> {
    Keys() {
      super(FilteredEntryMultimap.this);
    }

    @Override
    public int remove(@Nullable Object key, int occurrences) {
      Multisets.checkNonnegative(occurrences, "occurrences");
      if (occurrences == 0) {
        return count(key);
      }
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return 0;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      int oldCount = 0;
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          oldCount++;
          if (oldCount <= occurrences) {
            itr.remove();
          }
        }
      }
      return oldCount;
    }

    @Override
    public Set<Multiset.Entry<K>> entrySet() {
      return new Multisets.EntrySet<K>() {

        @Override
        Multiset<K> multiset() {
          return Keys.this;
        }

        @Override
        public Iterator<Multiset.Entry<K>> iterator() {
          return Keys.this.entryIterator();
        }

        @Override
        public int size() {
          return FilteredEntryMultimap.this.keySet().size();
        }
        
        private boolean removeIf(final Predicate<? super Multiset.Entry<K>> predicate) {
          return FilteredEntryMultimap.this.removeIf(new Predicate<Map.Entry<K, Collection<V>>>() {
            @Override
            public boolean apply(Map.Entry<K, Collection<V>> entry) {
              return predicate.apply(
                  Multisets.immutableEntry(entry.getKey(), entry.getValue().size()));
            }
          });
        }
        
        @Override
        public boolean removeAll(Collection<?> c) {
          return removeIf(in(c));
        }
        
        @Override
        public boolean retainAll(Collection<?> c) {
          return removeIf(not(in(c)));
        }
      };
    }
  }
}