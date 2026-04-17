package net.laffyco.javamatchingengine.core.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

/**
 * The matching engine.
 *
 * @author Laffini
 *
 */
@Component
public class MatchingEngine {

    /**
     * Set of asking buy orders. Thread-safe sorted set with highest price first.
     */
    private ConcurrentSkipListSet<Order> buyOrders;

    /**
     * Set of asking sell orders. Thread-safe sorted set with lowest price first.
     */
    private ConcurrentSkipListSet<Order> sellOrders;

    /**
     * Last sale price.
     */
    private double lastSalePrice;

    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(MatchingEngine.class.getName());

    /**
     * Constructor. Initializes thread-safe sorted sets for orders.
     */
    public MatchingEngine() {
        this.buyOrders = new ConcurrentSkipListSet<>();
        this.sellOrders = new ConcurrentSkipListSet<>();
    }

    /**
     * Place an order with validation. Thread-safe.
     *
     * @param order the order to place
     * @return the trades generated
     */
    public synchronized List<Trade> placeOrder(final Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getAmount() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        return this.process(order);
    }

    /**
     * Process an order and return the trades generated before adding the
     * remaining amount to the market.
     *
     * @param pOrder
     * @return trades
     */
    public synchronized List<Trade> process(final Order pOrder) {
        if (pOrder.getSide() == Side.BUY) {
            return this.processLimitBuy(pOrder);
        } else {
            return this.processLimitSell(pOrder);
        }
    }

    /**
     * Process limit buy.
     *
     * @param order
     * @return trades
     */
    private synchronized List<Trade> processLimitBuy(final Order order) {
        final ArrayList<Trade> trades = new ArrayList<>();

        final int n = this.sellOrders.size();

        // Check if order book is empty.
        if (n != 0) {
            // Check if at least one matching order.
            if (this.sellOrders.last().getPrice() <= order.getPrice()) {

                // Traverse matching orders
                while (!this.sellOrders.isEmpty()) {
                    final Order sellOrder = this.sellOrders.first();
                    if (sellOrder.getPrice() > order.getPrice()) {
                        break;
                    }
                    // Fill entire order.
                    if (sellOrder.getAmount() >= order.getAmount()) {
                        trades.add(new Trade(order.getId(), sellOrder.getId(),
                                order.getAmount(), sellOrder.getPrice()));
                        logger.info("Trade Executed: " + order.getAmount() + " @ " + sellOrder.getPrice());
                        sellOrder.setAmount(
                                sellOrder.getAmount() - order.getAmount());
                        if (sellOrder.getAmount() == 0) {
                            this.sellOrders.pollFirst();
                        }
                        this.setLastSalePrice(sellOrder.getPrice());
                        return trades;
                    }

                    // Fill partial order & continue.
                    if (sellOrder.getAmount() < order.getAmount()) {
                        trades.add(new Trade(order.getId(), sellOrder.getId(),
                                sellOrder.getAmount(), sellOrder.getPrice()));
                        logger.info("Trade Executed: " + sellOrder.getAmount() + " @ " + sellOrder.getPrice());
                        order.setAmount(
                                order.getAmount() - sellOrder.getAmount());
                        this.sellOrders.pollFirst();
                        this.setLastSalePrice(sellOrder.getPrice());
                        continue;
                    }
                }
            }
        }

        // Add remaining order to book.
        this.buyOrders.add(order);

        return trades;
    }

    /**
     * Process a limit sell.
     *
     * @param order
     * @return Trades.
     */
    private synchronized List<Trade> processLimitSell(final Order order) {

        final ArrayList<Trade> trades = new ArrayList<>();

        final int n = this.buyOrders.size();

        // Check if order book is empty.
        double currentPrice;
        if (n == 0) {
            currentPrice = -1;
        } else {
            currentPrice = this.buyOrders.last().getPrice();
        }

        // Check that there is at least one matching order.
        if (n != 0 && currentPrice >= order.getPrice()) {
            // Traverse all matching orders.
            while (!this.buyOrders.isEmpty()) {
                final Order buyOrder = this.buyOrders.first();

                // Fill entire order.
                if (buyOrder.getAmount() >= order.getAmount()) {
                    trades.add(new Trade(order.getId(), buyOrder.getId(),
                            order.getAmount(), buyOrder.getPrice()));
                    logger.info("Trade Executed: " + order.getAmount() + " @ " + buyOrder.getPrice());
                    buyOrder.setAmount(
                            buyOrder.getAmount() - order.getAmount());
                    if (buyOrder.getAmount() == 0) {
                        this.buyOrders.pollFirst();
                    }
                    this.setLastSalePrice(buyOrder.getPrice());
                    return trades;
                }

                // Fill partial order and continue.
                if (buyOrder.getAmount() < order.getAmount()) {
                    trades.add(new Trade(order.getId(), buyOrder.getId(),
                            buyOrder.getAmount(), buyOrder.getPrice()));
                    logger.info("Trade Executed: " + buyOrder.getAmount() + " @ " + buyOrder.getPrice());
                    order.setAmount(order.getAmount() - buyOrder.getAmount());
                    this.buyOrders.pollFirst();
                    this.setLastSalePrice(buyOrder.getPrice());
                    continue;
                }
            }
        }
        // Add remaining order to the list.
        this.sellOrders.add(order);

        return trades;
    }

    /**
     * Calculate the spread.
     *
     * @return Difference in buy and sell books.
     */
    public double getSpread() {

        if (this.buyOrders.size() != 0 && this.sellOrders.size() != 0) {
            final double buyOrderPrice = this.buyOrders.last().getPrice();

            final double sellOrderPrice = this.sellOrders.first().getPrice();

            return sellOrderPrice - buyOrderPrice;
        }
        return 0;
    }

    /**
     * Find an order by ID.
     *
     * @param id
     * @param side
     * @return the order (or null if not found)
     */
    public synchronized Order findOrder(final String id, final Side side) {
        List<Order> toSearch;
        if (side == Side.BUY) {
            toSearch = this.buyOrders;
        } else {
            toSearch = this.sellOrders;
        }
        return toSearch.stream().filter(order -> order.getId().equals(id))
                .findFirst().orElse(null);
    }

    /**
     * Cancel an order when the side is known.
     *
     * @param orderId
     * @param side
     * @return is order cancelled.
     */
    public synchronized boolean cancelOrder(final String orderId,
            final Side side) {
        if (side == Side.BUY) {
            // Search buy orders.
            return this.cancel(orderId, this.buyOrders);
        } else if (side == Side.SELL) {
            // Search sell orders.
            return this.cancel(orderId, this.sellOrders);
        } else {
            return false;
        }
    }

    /**
     * Cancel an order from an order book.
     *
     * @param orderId
     * @param orderBook
     * @return whether an order has been cancelled
     */
    private synchronized boolean cancel(final String orderId,
            final ConcurrentSkipListSet<Order> orderBook) {
        // Loop through order book to find order.
        for (Order order : orderBook) {
            if (order.getId().equals(orderId)) {
                orderBook.remove(order);
                return true; // Order cancelled.
            }
        }
        return false; // No order cancelled.
    }

    /**
     * @return the buyOrders
     */
    public synchronized List<Order> getBuyOrders() {
        return new ArrayList<>(this.buyOrders);
    }

    /**
     * @param pBuyOrders the buyOrders to set
     */
    public synchronized void setBuyOrders(final ArrayList<Order> pBuyOrders) {
        this.buyOrders.clear();
        this.buyOrders.addAll(pBuyOrders);
    }

    /**
     * @return the sellOrders
     */
    public synchronized List<Order> getSellOrders() {
        return new ArrayList<>(this.sellOrders);
    }

    /**
     * @param pSellOrders the sellOrders to set
     */
    public synchronized void setSellOrders(final ArrayList<Order> pSellOrders) {
        this.sellOrders.clear();
        this.sellOrders.addAll(pSellOrders);
    }

    /**
     * @return the lastSalePrice
     */
    public synchronized double getLastSalePrice() {
        return this.lastSalePrice;
    }

    /**
     * @param pLastSalePrice the lastSalePrice to set
     */
    public synchronized void setLastSalePrice(final double pLastSalePrice) {
        this.lastSalePrice = pLastSalePrice;
    }
}
