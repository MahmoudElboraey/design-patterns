package com.learn.productionrefactor.best;

public record OrderPlacedEvent(Order order, User user) {
}
