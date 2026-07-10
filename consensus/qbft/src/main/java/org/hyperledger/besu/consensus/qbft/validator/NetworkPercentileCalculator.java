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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkPercentileCalculator {

  public double percentile(final List<Double> values, final double percentile) {
    if (values == null || values.isEmpty()) {
      return 0.0;
    }

    final List<Double> sortedValues = new ArrayList<>(values);
    Collections.sort(sortedValues);

    final double index = (percentile / 100.0) * (sortedValues.size() - 1);
    final int lowerIndex = (int) Math.floor(index);
    final int upperIndex = (int) Math.ceil(index);

    if (lowerIndex == upperIndex) {
      return sortedValues.get(lowerIndex);
    }

    final double weight = index - lowerIndex;

    return sortedValues.get(lowerIndex) * (1.0 - weight) + sortedValues.get(upperIndex) * weight;
  }
}
