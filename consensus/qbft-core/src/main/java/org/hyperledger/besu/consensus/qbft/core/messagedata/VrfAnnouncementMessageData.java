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

  public static VrfAnnouncementMessageData fromMessageData(final MessageData messageData) {

    return fromMessageData(
        messageData,
        MESSAGE_CODE,
        VrfAnnouncementMessageData.class,
        VrfAnnouncementMessageData::new);
  }

  public VrfAnnouncementMessage decode() {
    return VrfAnnouncementMessage.decode(data);
  }

  public static VrfAnnouncementMessageData create(final VrfAnnouncementMessage announcement) {

    return new VrfAnnouncementMessageData(announcement.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
