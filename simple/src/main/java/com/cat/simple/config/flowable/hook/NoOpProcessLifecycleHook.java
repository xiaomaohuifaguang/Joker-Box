package com.cat.simple.config.flowable.hook;

import org.springframework.stereotype.Component;

/**
 * No-op implementation of ProcessLifecycleHook.
 * Provides a default bean when no custom hook is registered.
 */
@Component
public class NoOpProcessLifecycleHook implements ProcessLifecycleHook {
}
