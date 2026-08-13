package com.cat.simple.config.flowable.gateway.operator;

public class GtOperator extends AbstractRelationalOperator {

    public GtOperator() {
        super(">", result -> result > 0);
    }
}
