package com.learn.productionrefactor.after;

import com.learn.productionrefactor.before.Cart;

import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private List<OrderCreation> orderCreationObservers = new ArrayList<>();

    public void subscribeToOrderCreation(OrderCreation creation) {
        orderCreationObservers.add(creation);
    }

    public void unsubscribeFromOrderCreation(OrderCreation creation) {
        orderCreationObservers.remove(creation);
    }


    public Order placeOrder(Cart cart, User user) {
        Order order = createOrder(cart, user);
//
//        chargePayment(order);                           // core domain logic
//
//        emailClient.sendConfirmation(user.getEmail(), order);   // side effect
//        analytics.track("order_placed", order.getId());         // side effect
//        loyalty.addPoints(user.getId(), order.getTotal());      // side effect
//        warehouse.reserveStock(order.getItems());               // side effect

        notifyObservers(order , user);

        return order;
    }

    private void notifyObservers(Order order, User user) {

        for (OrderCreation observer : orderCreationObservers) {
            try {
                observer.onOrderCreated(order, user);
            }catch (Exception e) {
                if (observer instanceof WarehouseService) {
                    throw new RuntimeException("order creation failed");
                }
            }
        }
    }

    private  Order createOrder(Cart cart, User user) {
        return new Order("55" , 260.5);
    }


}
