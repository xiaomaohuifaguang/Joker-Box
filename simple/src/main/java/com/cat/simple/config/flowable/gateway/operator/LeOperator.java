package com.cat.simple.config.flowable.gateway.operator;

public class LeOperator extends AbstractRelationalOperator {

    public LeOperator() {
        super("<=", result -> result <= 0);
    }
}
