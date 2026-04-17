package net.laffyco.javamatchingengine.core.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.laffyco.javamatchingengine.core.engine.MatchingEngine;
import net.laffyco.javamatchingengine.core.engine.Order;

/**
 * Order controller.
 *
 * @author Laffini
 *
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * Matching engine.
     */
    private final MatchingEngine matchingEngine = new MatchingEngine();

    /**
     * Place an order.
     *
     * @param order the order to place
     * @return success message
     */
    @PostMapping
    public String placeOrder(@RequestBody final Order order) {
        this.matchingEngine.placeOrder(order);
        return "Order placed successfully";
    }

}