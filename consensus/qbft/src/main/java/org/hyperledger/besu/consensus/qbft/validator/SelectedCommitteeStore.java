/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the locally computed reputation committee for each block height. */
public class SelectedCommitteeStore {

  private final Map<Long, List<Address>> committees = new ConcurrentHashMap<>();

  public void put(final long blockHeight, final Collection<Address> validators) {

    committees.put(blockHeight, validators.stream().sorted().toList());
  }

  public List<Address> get(final long blockHeight) {
    return committees.getOrDefault(blockHeight, List.of());
  }

  public void remove(final long blockHeight) {
    committees.remove(blockHeight);
  }
}
