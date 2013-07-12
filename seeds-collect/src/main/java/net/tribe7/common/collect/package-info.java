/*
 * Copyright (C) 2007 The Guava Authors
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

/**
 * This package contains generic collection interfaces and implementations, and
 * other utilities for working with collections. It is a part of the open-source
 * <a href="http://guava-libraries.googlecode.com">Guava libraries</a>.
 *
 * <h2>Collection Types</h2>
 *
 * <dl>
 * <dt>{@link net.tribe7.common.collect.BiMap}
 * <dd>An extension of {@link java.util.Map} that guarantees the uniqueness of
 *     its values as well as that of its keys. This is sometimes called an
 *     "invertible map," since the restriction on values enables it to support
 *     an {@linkplain net.tribe7.common.collect.BiMap#inverse inverse view} --
 *     which is another instance of {@code BiMap}.
 *
 * <dt>{@link net.tribe7.common.collect.Multiset}
 * <dd>An extension of {@link java.util.Collection} that may contain duplicate
 *     values like a {@link java.util.List}, yet has order-independent equality
 *     like a {@link java.util.Set}.  One typical use for a multiset is to
 *     represent a histogram.
 *
 * <dt>{@link net.tribe7.common.collect.Multimap}
 * <dd>A new type, which is similar to {@link java.util.Map}, but may contain
 *     multiple entries with the same key. Some behaviors of
 *     {@link net.tribe7.common.collect.Multimap} are left unspecified and are
 *     provided only by the subtypes mentioned below.
 *
 * <dt>{@link net.tribe7.common.collect.ListMultimap}
 * <dd>An extension of {@link net.tribe7.common.collect.Multimap} which permits
 *     duplicate entries, supports random access of values for a particular key,
 *     and has <i>partially order-dependent equality</i> as defined by
 *     {@link net.tribe7.common.collect.ListMultimap#equals(Object)}. {@code
 *     ListMultimap} takes its name from the fact that the {@linkplain
 *     net.tribe7.common.collect.ListMultimap#get collection of values}
 *     associated with a given key fulfills the {@link java.util.List} contract.
 *
 * <dt>{@link net.tribe7.common.collect.SetMultimap}
 * <dd>An extension of {@link net.tribe7.common.collect.Multimap} which has
 *     order-independent equality and does not allow duplicate entries; that is,
 *     while a key may appear twice in a {@code SetMultimap}, each must map to a
 *     different value.  {@code SetMultimap} takes its name from the fact that
 *     the {@linkplain net.tribe7.common.collect.SetMultimap#get collection of
 *     values} associated with a given key fulfills the {@link java.util.Set}
 *     contract.
 *
 * <dt>{@link net.tribe7.common.collect.SortedSetMultimap}
 * <dd>An extension of {@link net.tribe7.common.collect.SetMultimap} for which
 *     the {@linkplain net.tribe7.common.collect.SortedSetMultimap#get
 *     collection values} associated with a given key is a
 *     {@link java.util.SortedSet}.
 *
 * <dt>{@link net.tribe7.common.collect.Table}
 * <dd>A new type, which is similar to {@link java.util.Map}, but which indexes
 *     its values by an ordered pair of keys, a row key and column key.
 *
 * <dt>{@link net.tribe7.common.collect.ClassToInstanceMap}
 * <dd>An extension of {@link java.util.Map} that associates a raw type with an
 *     instance of that type.
 * </dl>
 *
 * <h2>Collection Implementations</h2>
 *
 * <h3>of {@link java.util.List}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableList}
 * </ul>
 *
 * <h3>of {@link java.util.Set}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableSet}
 * <li>{@link net.tribe7.common.collect.ImmutableSortedSet}
 * <li>{@link net.tribe7.common.collect.ContiguousSet} (see {@code Range})
 * </ul>
 *
 * <h3>of {@link java.util.Map}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableMap}
 * <li>{@link net.tribe7.common.collect.ImmutableSortedMap}
 * <li>{@link net.tribe7.common.collect.MapMaker}
 * </ul>
 *
 * <h3>of {@link net.tribe7.common.collect.BiMap}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableBiMap}
 * <li>{@link net.tribe7.common.collect.HashBiMap}
 * <li>{@link net.tribe7.common.collect.EnumBiMap}
 * <li>{@link net.tribe7.common.collect.EnumHashBiMap}
 * </ul>
 *
 * <h3>of {@link net.tribe7.common.collect.Multiset}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableMultiset}
 * <li>{@link net.tribe7.common.collect.HashMultiset}
 * <li>{@link net.tribe7.common.collect.LinkedHashMultiset}
 * <li>{@link net.tribe7.common.collect.TreeMultiset}
 * <li>{@link net.tribe7.common.collect.EnumMultiset}
 * <li>{@link net.tribe7.common.collect.ConcurrentHashMultiset}
 * </ul>
 *
 * <h3>of {@link net.tribe7.common.collect.Multimap}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableMultimap}
 * <li>{@link net.tribe7.common.collect.ImmutableListMultimap}
 * <li>{@link net.tribe7.common.collect.ImmutableSetMultimap}
 * <li>{@link net.tribe7.common.collect.ArrayListMultimap}
 * <li>{@link net.tribe7.common.collect.HashMultimap}
 * <li>{@link net.tribe7.common.collect.TreeMultimap}
 * <li>{@link net.tribe7.common.collect.LinkedHashMultimap}
 * <li>{@link net.tribe7.common.collect.LinkedListMultimap}
 * </ul>
 *
 * <h3>of {@link net.tribe7.common.collect.Table}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableTable}
 * <li>{@link net.tribe7.common.collect.ArrayTable}
 * <li>{@link net.tribe7.common.collect.HashBasedTable}
 * <li>{@link net.tribe7.common.collect.TreeBasedTable}
 * </ul>
 *
 * <h3>of {@link net.tribe7.common.collect.ClassToInstanceMap}</h3>
 * <ul>
 * <li>{@link net.tribe7.common.collect.ImmutableClassToInstanceMap}
 * <li>{@link net.tribe7.common.collect.MutableClassToInstanceMap}
 * </ul>
 *
 * <h2>Classes of static utility methods</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.Collections2}
 * <li>{@link net.tribe7.common.collect.Iterators}
 * <li>{@link net.tribe7.common.collect.Iterables}
 * <li>{@link net.tribe7.common.collect.Lists}
 * <li>{@link net.tribe7.common.collect.Maps}
 * <li>{@link net.tribe7.common.collect.Queues}
 * <li>{@link net.tribe7.common.collect.Sets}
 * <li>{@link net.tribe7.common.collect.Multisets}
 * <li>{@link net.tribe7.common.collect.Multimaps}
 * <li>{@link net.tribe7.common.collect.Tables}
 * <li>{@link net.tribe7.common.collect.ObjectArrays}
 * </ul>
 *
 * <h2>Comparison</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.Ordering}
 * <li>{@link net.tribe7.common.collect.ComparisonChain}
 * </ul>
 *
 * <h2>Abstract implementations</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.AbstractIterator}
 * <li>{@link net.tribe7.common.collect.AbstractSequentialIterator}
 * <li>{@link net.tribe7.common.collect.ImmutableCollection}
 * <li>{@link net.tribe7.common.collect.UnmodifiableIterator}
 * <li>{@link net.tribe7.common.collect.UnmodifiableListIterator}
 * </ul>
 *
 * <h2>Ranges</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.Range}
 * <li>{@link net.tribe7.common.collect.DiscreteDomain}
 * <li>{@link net.tribe7.common.collect.DiscreteDomains}
 * <li>{@link net.tribe7.common.collect.ContiguousSet}
 * </ul>
 *
 * <h2>Other</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.Interner},
 *     {@link net.tribe7.common.collect.Interners}
 * <li>{@link net.tribe7.common.collect.Constraint},
 *     {@link net.tribe7.common.collect.Constraints}
 * <li>{@link net.tribe7.common.collect.MapConstraint},
 *     {@link net.tribe7.common.collect.MapConstraints}
 * <li>{@link net.tribe7.common.collect.MapDifference},
 *     {@link net.tribe7.common.collect.SortedMapDifference}
 * <li>{@link net.tribe7.common.collect.MinMaxPriorityQueue}
 * <li>{@link net.tribe7.common.collect.PeekingIterator}
 * </ul>
 *
 * <h2>Forwarding collections</h2>
 *
 * <ul>
 * <li>{@link net.tribe7.common.collect.ForwardingCollection}
 * <li>{@link net.tribe7.common.collect.ForwardingConcurrentMap}
 * <li>{@link net.tribe7.common.collect.ForwardingIterator}
 * <li>{@link net.tribe7.common.collect.ForwardingList}
 * <li>{@link net.tribe7.common.collect.ForwardingListIterator}
 * <li>{@link net.tribe7.common.collect.ForwardingListMultimap}
 * <li>{@link net.tribe7.common.collect.ForwardingMap}
 * <li>{@link net.tribe7.common.collect.ForwardingMapEntry}
 * <li>{@link net.tribe7.common.collect.ForwardingMultimap}
 * <li>{@link net.tribe7.common.collect.ForwardingMultiset}
 * <li>{@link net.tribe7.common.collect.ForwardingNavigableMap}
 * <li>{@link net.tribe7.common.collect.ForwardingNavigableSet}
 * <li>{@link net.tribe7.common.collect.ForwardingObject}
 * <li>{@link net.tribe7.common.collect.ForwardingQueue}
 * <li>{@link net.tribe7.common.collect.ForwardingSet}
 * <li>{@link net.tribe7.common.collect.ForwardingSetMultimap}
 * <li>{@link net.tribe7.common.collect.ForwardingSortedMap}
 * <li>{@link net.tribe7.common.collect.ForwardingSortedSet}
 * <li>{@link net.tribe7.common.collect.ForwardingSortedSetMultimap}
 * <li>{@link net.tribe7.common.collect.ForwardingTable}
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package net.tribe7.common.collect;
