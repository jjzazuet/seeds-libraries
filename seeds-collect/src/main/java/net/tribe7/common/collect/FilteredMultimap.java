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

import net.tribe7.common.annotations.GwtCompatible;
import net.tribe7.common.base.Predicate;

import java.util.Map.Entry;

/**
 * A superclass of all filtered multimap types.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class FilteredMultimap<K, V> extends AbstractMultimap<K, V> {
  final Multimap<K, V> unfiltered;
  
  FilteredMultimap(Multimap<K, V> unfiltered) {
    this.unfiltered = checkNotNull(unfiltered);
  }

  abstract Predicate<? super Entry<K, V>> entryPredicate();
}