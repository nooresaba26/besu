/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.core.types;

import org.hyperledger.besu.consensus.qbft.core.messagewrappers.VrfAnnouncementMessage;

/** Handles authenticated VRF announcements received from QBFT validators. */
@FunctionalInterface
public interface VrfAnnouncementHandler {

  /**
   * Handles a received VRF announcement.
   *
   * @param announcement signed VRF announcement
   */
  void handle(VrfAnnouncementMessage announcement);
}