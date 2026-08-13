package com.cat.simple.config.flowable.gateway.operator;

public class GeOperator extends AbstractRelationalOperator {

    public GeOperator() {
        super(">=", result -> result >= 0);
    }
}
