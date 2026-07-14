/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.core.messagedata;

import org.hyperledger.besu.consensus.common.bft.messagedata.AbstractBftMessageData;
import org.hyperledger.besu.consensus.qbft.core.messagewrappers.VrfAnnouncementMessage;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;

/** P2P message data for a signed VRF announcement. */
public class VrfAnnouncementMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = QbftV1.VRF_ANNOUNCEMENT;

  private VrfAnnouncementMessageData(final Bytes data) {
    super(data);
  }

  public static VrfAnnouncementMessageData fromMessageData(
      final MessageData messageData) {

    return fromMessageData(
        messageData,
        MESSAGE_CODE,
        VrfAnnouncementMessageData.class,
        VrfAnnouncementMessageData::new);
  }

  public VrfAnnouncementMessage decode() {
    return VrfAnnouncementMessage.decode(data);
  }

  public static VrfAnnouncementMessageData create(
      final VrfAnnouncementMessage announcement) {

    return new VrfAnnouncementMessageData(announcement.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}