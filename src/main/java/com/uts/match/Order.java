/*
 * Copyright 2018 nghiatc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uts.match;

/**
 *
 * @author nghiatc
 * @since Mar 1, 2018
 */
public class Order {

    private PriceLevel level;
    private String id;
    private long remainingQuantity;

    public Order(PriceLevel level, String id, long size) {
        this.level = level;
        this.id = id;
        this.remainingQuantity = size;
    }

    public PriceLevel getLevel() {
        return level;
    }

    public String getId() {
        return id;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public void reduce(long quantity) {
        remainingQuantity -= quantity;
    }

    public void resize(long size) {
        remainingQuantity = size;
    }

}
