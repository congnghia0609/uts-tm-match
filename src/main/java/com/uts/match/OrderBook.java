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

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;

/**
 *
 * @author nghiatc
 * @since Mar 1, 2018
 * 
 * An order book.
 */
public class OrderBook {

    private Long2ObjectRBTreeMap<PriceLevel> bids; // buy
    private Long2ObjectRBTreeMap<PriceLevel> asks; // sell

    private Object2ObjectOpenHashMap<String, Order> orders;
    private OrderBookListener listener;

    public Long2ObjectRBTreeMap<PriceLevel> getBids() {
        return bids;
    }

    public Long2ObjectRBTreeMap<PriceLevel> getAsks() {
        return asks;
    }

    public Object2ObjectOpenHashMap<String, Order> getOrders() {
        return orders;
    }

    /**
     * Create an order book.
     *
     * @param listener a listener for outbound events from the order book
     */
    public OrderBook(OrderBookListener listener) {
        this.bids = new Long2ObjectRBTreeMap<>(LongComparators.OPPOSITE_COMPARATOR);
        this.asks = new Long2ObjectRBTreeMap<>(LongComparators.NATURAL_COMPARATOR);

        this.orders = new Object2ObjectOpenHashMap<>();
        this.listener = listener;
    }

    /**
     * Enter an order to this order book.
     *
     * <p>The incoming order is first matched against resting orders in this
     * order book. This operation results in zero or more Match events.</p>
     *
     * <p>If the remaining quantity is not zero after the matching operation,
     * the remaining quantity is added to this order book and an Add event is
     * triggered.</p>
     *
     * <p>If the order identifier is known, do nothing.</p>
     *
     * @param orderId an order identifier
     * @param side the side
     * @param price the limit price
     * @param size the size
     */
    public void enter(String orderId, Side side, long price, long size) {
        if (orders.containsKey(orderId))
            return;

        if (side == Side.BUY){
            buy(orderId, price, size);
        } else if (side == Side.SELL){
            sell(orderId, price, size);
        }
    }

    private void buy(String orderId, long price, long size) {
        long remainingQuantity = size;
        PriceLevel bestLevel = getBestLevel(asks);

        while (remainingQuantity > 0 && bestLevel != null && bestLevel.getPrice() <= price) {
            remainingQuantity = bestLevel.match(orderId, Side.BUY, remainingQuantity, listener);
            if (bestLevel.isEmpty()){
                asks.remove(bestLevel.getPrice());
            }
            bestLevel = getBestLevel(asks);
        }

        if (remainingQuantity > 0) {
            orders.put(orderId, add(bids, orderId, Side.BUY, price, remainingQuantity));
            listener.add(orderId, Side.BUY, price, remainingQuantity);
        }
    }

    private void sell(String orderId, long price, long size) {
        long remainingQuantity = size;
        PriceLevel bestLevel = getBestLevel(bids);

        while (remainingQuantity > 0 && bestLevel != null && bestLevel.getPrice() >= price) {
            remainingQuantity = bestLevel.match(orderId, Side.SELL, remainingQuantity, listener);
            if (bestLevel.isEmpty()){
                bids.remove(bestLevel.getPrice());
            }
            bestLevel = getBestLevel(bids);
        }

        if (remainingQuantity > 0) {
            orders.put(orderId, add(asks, orderId, Side.SELL, price, remainingQuantity));
            listener.add(orderId, Side.SELL, price, remainingQuantity);
        }
    }

    /**
     * Cancel a quantity of an order in this order book. The size refers
     * to the new order size. If the new order size is set to zero, the
     * order is deleted from this order book.
     *
     * <p>A Cancel event is triggered.</p>
     *
     * <p>If the order identifier is unknown, do nothing.</p>
     *
     * @param orderId the order identifier
     * @param size the new size
     */
    public void cancel(String orderId, long size) {
        Order orderMap = orders.get(orderId);
        if (orderMap == null)
            return;

        Order order = getOrderTree(orderMap);
        if (order == null) {
            orders.remove(orderId);
            return;
        }
        long remainingQuantity = order.getRemainingQuantity();

        if (size >= remainingQuantity)
            return;
        if (size > 0) {
            order.resize(size);
        } else {
            delete(order);
            orders.remove(orderId);
        }
        listener.cancel(orderId, order.getLevel().getPrice(), remainingQuantity - size, size, order.getLevel().getSide());
    }
    
    public Order getOrderTree(Order order) {
        Order rs = null;
        long price = order.getLevel().getPrice();
        switch (order.getLevel().getSide()) {
        case BUY:
            if (bids != null && !bids.isEmpty()) {
                PriceLevel pl = bids.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o;
                                break;
                            }
                        }
                    }
                }
            }
            break;
        case SELL:
            if (asks != null && !asks.isEmpty()) {
                PriceLevel pl = asks.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o;
                                break;
                            }
                        }
                    }
                }
            }
            break;
        }
        return rs;
    }
    
    public Order getOrderTree(String orderId) {
        Order rs = null;
        Order order = orders.get(orderId);
        if (order == null) {
            return rs;
        }
        
        long price = order.getLevel().getPrice();
        switch (order.getLevel().getSide()) {
        case BUY:
            if (bids != null && !bids.isEmpty()) {
                PriceLevel pl = bids.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o;
                                break;
                            }
                        }
                    }
                }
            }
            break;
        case SELL:
            if (asks != null && !asks.isEmpty()) {
                PriceLevel pl = asks.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o;
                                break;
                            }
                        }
                    }
                }
            }
            break;
        }
        return rs;
    }
    
    public long getRemainQuantityOrder(Order order) {
        long rs = 0;
        long price = order.getLevel().getPrice();
        if (order.getLevel().getSide() == Side.BUY) {
            // Buy - Mua: Giam dan.
            if (bids != null && !bids.isEmpty()) {
                PriceLevel pl = bids.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o.getRemainingQuantity();
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            // Sell - Bán: Tang dan
            if (asks != null && !asks.isEmpty()) {
                PriceLevel pl = asks.get(price);
                if(pl != null){
                    List<Order> listOrder = pl.getOrders();
                    if(listOrder != null && !listOrder.isEmpty()) {
                        for(Order o : listOrder){
                            if (o.getId().equals(order.getId())) {
                                rs = o.getRemainingQuantity();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return rs;
    }

    private PriceLevel getBestLevel(Long2ObjectRBTreeMap<PriceLevel> levels) {
        if (levels.isEmpty())
            return null;
        return levels.get(levels.firstLongKey());
    }

    private Order add(Long2ObjectRBTreeMap<PriceLevel> levels, String orderId, Side side, long price, long size) {
        PriceLevel level = levels.get(price);
        if (level == null) {
            level = new PriceLevel(side, price);
            levels.put(price, level);
        }
        return level.add(orderId, size);
    }

    private void delete(Order order) {
        PriceLevel level = order.getLevel();
        level.delete(order);
        if (level.isEmpty())
            delete(level);
    }

    private void delete(PriceLevel level) {
        switch (level.getSide()) {
        case BUY:
            bids.remove(level.getPrice());
            break;
        case SELL:
            asks.remove(level.getPrice());
            break;
        }
    }

}
