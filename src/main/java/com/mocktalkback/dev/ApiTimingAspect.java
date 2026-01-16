package com.mocktalkback.dev;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@Profile("dev")
public class ApiTimingAspect {

  private static final long WARN_THRESHOLD_MS = 200;

  @Around("within(@org.springframework.web.bind.annotation.RestController *)")
  public Object timeController(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.nanoTime();
    try {
      Object ret = pjp.proceed();
      return ret;
    } finally {
      long tookMs = (System.nanoTime() - start) / 1_000_000;
      if (tookMs >= WARN_THRESHOLD_MS) {
        String sig = pjp.getSignature().toShortString();
        // 필요하면 MDC로 requestId/userId도 같이 찍기
        // log.warn("[API_SLOW] {} took={}ms", sig, tookMs);
        log.info("[API_SLOW] {} took={}ms", sig, tookMs);
      } else {
        // log.debug("[API] {} took={}ms", pjp.getSignature().toShortString(), tookMs);
        log.info("[API] {} took={}ms", pjp.getSignature().toShortString(), tookMs);
      }
    }
  }
}

