package com.cat.simple.config.flowable.gateway.operator;

public class LtOperator extends AbstractRelationalOperator {

    public LtOperator() {
        super("<", result -> result < 0);
    }
}
