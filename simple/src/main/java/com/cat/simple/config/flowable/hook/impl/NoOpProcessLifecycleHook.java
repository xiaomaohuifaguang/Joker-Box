package com.cat.simple.config.flowable.hook.impl;

import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of ProcessLifecycleHook.
 * Provides a default bean when no custom hook is registered.
 */
@Component
@Order(1)
public class NoOpProcessLifecycleHook implements ProcessLifecycleHook {
}
