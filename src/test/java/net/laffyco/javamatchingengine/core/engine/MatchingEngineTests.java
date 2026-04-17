package net.laffyco.javamatchingengine.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.annotation.Resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

/**
 * Matching engine tests.
 *
 * @author Laffini
 *
 */
public class MatchingEngineTests extends MatchingEngineTest {

    /**
     * Test amount.
     */
    private final double amount = 3;

    /**
     * Test price.
     */
    private final double price = 2;

    /**
     * Array of orders.
     */
    private final Order[] orders = {
            new Order.Builder(Side.BUY).withAmount(this.amount)
                    .atPrice(this.price).build(),
            new Order.Builder(Side.SELL).withAmount(this.amount)
                    .atPrice(this.price).build() };

    /**
     * Test MatchingEngine.
     */
    @InjectMocks
    @Resource
    private MatchingEngine matchingEngine;

    /**
     * Add a buy order, then add a matching sell order.
     */
    @Test
    @DisplayName("Add a buy order, then add a matching sell order")
    public void buyThenSell() {

        // Add buy order
        this.matchingEngine.placeOrder(this.orders[0]);

        // There should only be 1 buy order.
        assertEquals(this.matchingEngine.getBuyOrders().size(), 1);

        // There should not be any sell orders
        assertEquals(this.matchingEngine.getSellOrders().size(), 0);

        // Add sell order
        final List<Trade> trades = this.matchingEngine.placeOrder(this.orders[1]);

        // There should not be any buy or sell orders
        assertEquals(this.matchingEngine.getSellOrders().size(), 0);
        assertEquals(this.matchingEngine.getBuyOrders().size(), 0);

        // There should be only 1 trade
        assertEquals(trades.size(), 1);

        // Assert the trade details
        final Trade trade = trades.get(0);
        assertEquals(trade.getPrice(), this.orders[1].getPrice());
        assertEquals(trade.getAmount(), this.orders[1].getAmount());

        // Assert last trade price
        assertEquals(this.matchingEngine.getLastSalePrice(), 2);
    }

    /**
     * Add a sell order, then add a buy order.
     */
    @Test
    @DisplayName("Add a sell order, then add a matching buy order")
    public void sellThenBuy() {

        // Add sell order
        this.matchingEngine.placeOrder(this.orders[1]);

        // There should only be 1 sell order.
        assertEquals(this.matchingEngine.getSellOrders().size(), 1);

        // There should not be any buy orders
        assertEquals(this.matchingEngine.getBuyOrders().size(), 0);

        // Add buy order
        final List<Trade> trades = this.matchingEngine.placeOrder(this.orders[0]);

        // There should not be any buy or sell orders
        assertEquals(this.matchingEngine.getSellOrders().size(), 0);
        assertEquals(this.matchingEngine.getBuyOrders().size(), 0);

        // There should be only 1 trade
        assertEquals(trades.size(), 1);

        // Assert the trade details
        final Trade trade = trades.get(0);
        assertEquals(trade.getPrice(), this.orders[0].getPrice());
        assertEquals(trade.getAmount(), this.orders[0].getAmount());

        // Assert last trade price
        assertEquals(this.matchingEngine.getLastSalePrice(), 2);
    }

    /**
     * Find order tests.
     */
    @Test
    @DisplayName("Find orders")
    public void findOrder() {

        // Can't find an order that hasn't been added to the book.
        assertEquals(this.matchingEngine.findOrder(this.orders[1].getId(),
                this.orders[1].getSide()), null);
        assertEquals(this.matchingEngine.findOrder(this.orders[0].getId(),
                this.orders[0].getSide()), null);

        // Add sell order
        this.matchingEngine.placeOrder(this.orders[1]);

        // Can find the order now.
        assertEquals(this.matchingEngine.findOrder(this.orders[1].getId(),
                this.orders[1].getSide()), this.orders[1]);

        // Add & find buy order.
        this.matchingEngine.placeOrder(this.orders[0]);
        // Add twice as first is matched with previous sell.
        this.matchingEngine.placeOrder(this.orders[0]);
        assertEquals(this.matchingEngine.findOrder(this.orders[0].getId(),
                this.orders[0].getSide()), this.orders[0]);
    }

    /**
     * Test that no orders are cancelled when an invalid side is provided.
     */
    @Test
    @DisplayName("No orders are cancelled when an invalid side is provided")
    public void cancelTestInvalidSide() {

        final String orderId = "";

        final boolean result = this.matchingEngine.cancelOrder(orderId, null);
        assertFalse(result);
    }

    /**
     * A buy order that fills two sell orders.
     */
    @Test
    @DisplayName("A buy order that fills two sell orders")
    public void buyPartialFill() {

        // Add two sell orders.
        this.matchingEngine.placeOrder(this.orders[1]);
        this.matchingEngine.placeOrder(this.orders[1]);

        // Modify buy order to be twice the amount.
        final Order buyOrder = this.orders[0];
        buyOrder.setAmount(buyOrder.getAmount() * 2);

        // Add the buy order.
        final List<Trade> trades = this.matchingEngine.placeOrder(buyOrder);

        // Two trades should have taken place.
        assertTrue(trades.size() == 2);

        // Order book should be empty.
        assertTrue(this.matchingEngine.getBuyOrders().isEmpty());
        assertTrue(this.matchingEngine.getSellOrders().isEmpty());

        // The trades match the expected amt and price.
        for (final Trade trade : trades) {
            assertTrue(trade.getAmount() == buyOrder.getAmount());
            assertTrue(trade.getPrice() == buyOrder.getPrice());
        }
    }

    /**
     * A buy order that fills two sell orders.
     */
    @Test
    @DisplayName("A sell order that fills two buy orders")
    public void sellPartialFill() {

        // Add two buy orders.
        this.matchingEngine.placeOrder(this.orders[0]);
        this.matchingEngine.placeOrder(this.orders[0]);

        // Modify sell order to be twice the amount.
        final Order sellOrder = this.orders[1];
        sellOrder.setAmount(sellOrder.getAmount() * 2);

        // Add the buy order.
        final List<Trade> trades = this.matchingEngine.placeOrder(sellOrder);

        // Two trades should have taken place.
        assertTrue(trades.size() == 2);

        // Order book should be empty.
        assertTrue(this.matchingEngine.getBuyOrders().isEmpty());
        assertTrue(this.matchingEngine.getSellOrders().isEmpty());

        // The trades match the expected amt and price.
        for (final Trade trade : trades) {
            assertTrue(trade.getAmount() == sellOrder.getAmount());
            assertTrue(trade.getPrice() == sellOrder.getPrice());
        }
    }
}
