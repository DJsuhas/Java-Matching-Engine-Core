package net.laffyco.javamatchingengine.core.engine;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

/**
 * Common test functionality.
 *
 * @author Laffini
 *
 */
public abstract class MatchingEngineTest {

    /**
     * Test setup.
     */
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Add an array of orders to the matching engine.
     *
     * @param matchingEngine
     * @param orders
     */
    public void addOrders(final MatchingEngine matchingEngine, final Order[] orders) {
        for (int i = 0; i < orders.length; i++) {
            matchingEngine.placeOrder(orders[i]);
        }

    }
}
