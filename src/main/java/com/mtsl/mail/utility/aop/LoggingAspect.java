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
 * @author ajay.kumar10
 * Centralized logging using AOP.
 */
@Aspect
@Component
public class LoggingAspect {
	private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

	/**
	 * Logs execution details for methods in DAO, Service, and Utility layers.
	 */
	@Around("execution(* com.mtsl.mail.utility..*(..))")
	public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();

		// Extract method details
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		String className = methodSignature.getDeclaringType().getSimpleName();
		String methodName = methodSignature.getName();
		String fullMethodName = className + "." + methodName;
		Object[] args = joinPoint.getArgs();

		logger.info("\n============================== {}() Start ======================================================================================================", fullMethodName);
		if (logger.isInfoEnabled()) {
			logger.info("Entering method -> {}(); :: Arguments: {}", fullMethodName, Arrays.toString(args));
		}

		Object result;
		try {
			result = joinPoint.proceed();
			long elapsedTime = System.currentTimeMillis() - startTime;

			// Log successful execution with return value
			logger.info("Method executed -> {}(); in {} ms", fullMethodName, elapsedTime);
			logger.info("Returned response.");
		} catch (Exception ex) {
			logger.info("\n\n============================== {}(); End ========================================================================================================", "Exception Block Start.");
			StackTraceElement[] stackTrace = ex.getStackTrace();
			if (stackTrace.length > 0) {
				StackTraceElement element = stackTrace[0];
				logger.error("Exception in method -> {}(); at {}:{} - {}", fullMethodName, element.getFileName(),
						element.getLineNumber(), ex.getMessage());
			} else {
				logger.error("Exception in method -> {}(); - {}", fullMethodName, ex.getMessage());
			}
			logger.error("\nFull Stack Trace:", ex);
			logger.info("\n\n============================== {}(); End ========================================================================================================", "Exception Block End.");
			throw ex;
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		logger.info("Execution Time: {} ms", elapsedTime);
		logger.info("\n============================== {}() End ========================================================================================================", fullMethodName);
		return result;
	}
}
