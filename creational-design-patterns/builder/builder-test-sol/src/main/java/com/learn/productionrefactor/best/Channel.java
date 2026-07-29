package com.learn.productionrefactor.best;

public enum Channel {
    EMAIL,
    SMS,
    PUSH;

    public boolean supportsSubject() {
        return this == EMAIL;
    }
}
