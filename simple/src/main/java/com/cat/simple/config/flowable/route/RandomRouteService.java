package com.cat.simple.config.flowable.route;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机路由服务，用于 BPMN 表达式测试。
 * 在 sequenceFlow 的 conditionExpression 中调用，随机决定走向。
 *
 * <p>用法示例：</p>
 * <pre>{@code
 *   <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
 *       ${randomRouteService.random()}
 *   </bpmn:conditionExpression>
 * }</pre>
 */
@Component("randomRouteService")
public class RandomRouteService {

    /**
     * 随机返回 true / false，概率各 50%。
     */
    public boolean random() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * 按指定概率返回 true。
     *
     * @param percent true 的概率，范围 0.0 ~ 1.0
     */
    public boolean random(double percent) {
        return ThreadLocalRandom.current().nextDouble() < percent;
    }

    /**
     * 随机返回 true / false，并打印当前执行上下文（调试用）。
     */
    public boolean randomWithLog(DelegateExecution execution) {
        boolean result = ThreadLocalRandom.current().nextBoolean();
        System.out.printf("[RandomRoute] processInstanceId=%s, activityId=%s, result=%s%n",
                execution.getProcessInstanceId(),
                execution.getCurrentActivityId(),
                result);
        return result;
    }
}
