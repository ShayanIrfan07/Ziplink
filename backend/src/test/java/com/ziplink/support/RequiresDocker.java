package com.ziplink.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class RequiresDocker implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		try {
			if (org.testcontainers.DockerClientFactory.instance().isDockerAvailable()) {
				return ConditionEvaluationResult.enabled("Docker is available");
			}
			return ConditionEvaluationResult.disabled("Docker is not available");
		} catch (Throwable exception) {
			return ConditionEvaluationResult.disabled("Docker is not available");
		}
	}
}
