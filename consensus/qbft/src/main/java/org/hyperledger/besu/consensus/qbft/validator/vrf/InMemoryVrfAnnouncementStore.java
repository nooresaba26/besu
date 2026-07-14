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
package org.hyperledger.besu.consensus.qbft.validator.vrf;

import org.hyperledger.besu.datatypes.Address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryVrfAnnouncementStore implements VrfAnnouncementStore {

  private final ConcurrentHashMap<Long, ConcurrentHashMap<Address, VrfAnnouncement>>
      announcementsByBlock = new ConcurrentHashMap<>();

  @Override
  public void put(final VrfAnnouncement announcement) {
    if (announcement == null) {
      throw new IllegalArgumentException("VRF announcement cannot be null");
    }

    announcementsByBlock
        .computeIfAbsent(announcement.blockHeight(), ignored -> new ConcurrentHashMap<>())
        .put(announcement.validator(), announcement);
  }

  @Override
  public Collection<VrfAnnouncement> getAnnouncements(final long blockHeight) {
    final Map<Address, VrfAnnouncement> announcements = announcementsByBlock.get(blockHeight);

    if (announcements == null || announcements.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(new ArrayList<>(announcements.values()));
  }

  @Override
  public VrfAnnouncement getAnnouncement(final long blockHeight, final Address validator) {

    if (validator == null) {
      throw new IllegalArgumentException("Validator address cannot be null");
    }

    final Map<Address, VrfAnnouncement> announcements = announcementsByBlock.get(blockHeight);

    if (announcements == null) {
      return null;
    }

    return announcements.get(validator);
  }

  @Override
  public void clear(final long blockHeight) {
    announcementsByBlock.remove(blockHeight);
  }
}
