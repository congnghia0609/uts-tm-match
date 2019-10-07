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

import java.util.*;

/**
 *
 * @author nghiatc
 * @since Mar 1, 2018
 */
public class PriceLevel {

    private Side side;
    private long price;
    private List<Order> orders;

    public PriceLevel(Side side, long price) {
        this.side   = side;
        this.price  = price;
        this.orders = new ArrayList<>();
    }

    public Side getSide() {
        return side;
    }

    public long getPrice() {
        return price;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public Order add(String orderId, long size) {
        Order order = new Order(this, orderId, size);
        orders.add(order);
        return order;
    }

    public long match(String orderId, Side side, long quantity, OrderBookListener listener) {
        while (quantity > 0 && !orders.isEmpty()) {
            Order order = orders.get(0);
            long orderQuantity = order.getRemainingQuantity();

            if (orderQuantity > quantity) {
                order.reduce(quantity);
                listener.match(order.getId(), orderId, side, price, quantity, order.getRemainingQuantity());
                quantity = 0;
            } else {
                orders.remove(0);
                listener.match(order.getId(), orderId, side, price, orderQuantity, 0);
                quantity -= orderQuantity;
            }
        }
        return quantity;
    }

    public void delete(Order order) {
        orders.remove(order);
    }

}
