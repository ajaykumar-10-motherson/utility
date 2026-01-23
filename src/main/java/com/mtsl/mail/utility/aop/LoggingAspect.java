/**
 * 
 */
package com.mtsl.mail.utility.aop;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author ajay.kumar10 Centralized logging using AOP.
 */
@Aspect
@Component
public class LoggingAspect {

	private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

	@Around("execution(* com.mtsl.mail.utility..*(..))")
	public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

		long startTime = System.currentTimeMillis();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();

		String className = signature.getDeclaringType().getSimpleName();
		String methodName = signature.getName();
		String fullMethodName = className + "." + methodName;

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Entering method: {} with args: {}", fullMethodName, Arrays.toString(joinPoint.getArgs()));
			}

			return joinPoint.proceed();

		} catch (Throwable ex) {

			logger.error("Exception in {}: {}", fullMethodName, ex.getMessage(), ex);
			throw ex;

		} finally {

			long executionTime = System.currentTimeMillis() - startTime;
			if (executionTime > 500) {
			    logger.warn("Slow method detected: {} took {} ms", fullMethodName, executionTime);
			}
		}
	}
}
