package net.laffyco.javamatchingengine.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

/**
 * Test the getSpread() functionality.
 *
 * @author Laffini
 */
public class SpreadTests extends MatchingEngineTest {

    /**
     * Matching engine.
     */
    @InjectMocks
    @Resource
    private MatchingEngine matchingEngine;

    @Test
    @DisplayName("Get the spread when there are two orders")
    void spreadTwoOrders() {
        final Order[] orders = {
                new Order.Builder(Side.SELL).withAmount(2).atPrice(2.5)
                        .withId("sellOrder").build(),
                new Order.Builder(Side.BUY).withAmount(2).atPrice(2)
                        .withId("buyOrder").build() };
        this.addOrders(this.matchingEngine, orders);

        final double expectedSpread = 0.5;

        assertEquals(expectedSpread, this.matchingEngine.getSpread());
    }

    @Test
    @DisplayName("Get the spread when there are multiple orders")
    void spreadMultipleOrders() {
        final Order[] orders = {
                new Order.Builder(Side.SELL).withAmount(2).atPrice(2.5)
                        .withId("sellOrder").build(),
                new Order.Builder(Side.SELL).withAmount(2).atPrice(2.75)
                        .withId("secondSellOrder").build(),
                new Order.Builder(Side.BUY).withAmount(2).atPrice(2)
                        .withId("buyOrder").build(),
                new Order.Builder(Side.BUY).withAmount(2).atPrice(1.5)
                        .withId("secondBuyOrder").build() };
        this.addOrders(this.matchingEngine, orders);

        final double expectedSpread = 0.5;

        assertEquals(expectedSpread, this.matchingEngine.getSpread());
    }

    @Test
    @DisplayName("Attempt to get the spread when there are no orders")
    void spreadNoOrders() {
        assertEquals(this.matchingEngine.getSpread(), 0);
    }

}
