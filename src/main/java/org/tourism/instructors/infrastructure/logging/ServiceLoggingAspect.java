package org.tourism.instructors.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* org.tourism.instructors.application..*(..))")
    public Object logServiceCall(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("{} completed in {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("{} failed: {}", method, e.getMessage());
            throw e;
        }

    }
}
