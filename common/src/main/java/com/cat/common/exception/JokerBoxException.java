package com.cat.common.exception;

public class JokerBoxException extends RuntimeException {

    public JokerBoxException(String message) {
        super(message);   // 不填 cause,业务异常不需要堆栈追溯 cause
    }

    public JokerBoxException(String message, Throwable cause) {
        super(message, cause);   // 不填 cause,业务异常不需要堆栈追溯 cause
    }


}
