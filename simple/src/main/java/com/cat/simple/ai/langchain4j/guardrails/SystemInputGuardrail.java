package com.cat.simple.ai.langchain4j.guardrails;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

@Component
public class SystemInputGuardrail implements InputGuardrail {


    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        return success();
    }
}
