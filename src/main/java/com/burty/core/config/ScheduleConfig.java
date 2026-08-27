package com.burty.core.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄링 / 비동기 실행 설정.
 *
 * <p>예전에는 {@code @EnableScheduling} 만 있고 {@code TaskScheduler} 빈이 없었다. 그러면 스프링 기본 풀 크기는 <b>1</b>
 * 이다. 그런데 이 애플리케이션에는 크론 배치 5개와 <b>1초마다 도는 큐 폴러</b>가 같이 있었다. 매달 1일 월간 리포트 배치가 도는 동안 알림 큐가 통째로 멈추고,
 * 반대로 큐 폴러의 블로킹 읽기가 배치 시작을 지연시켰다.
 *
 * <p>스케줄러 풀을 늘리고, 애플리케이션 종료 시 진행 중인 작업이 끝날 때까지 기다리도록 설정한다.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class ScheduleConfig {

  @Bean
  public ThreadPoolTaskScheduler taskScheduler(
      @org.springframework.beans.factory.annotation.Value("${burty.scheduling.pool-size:8}")
          int poolSize) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(poolSize);
    scheduler.setThreadNamePrefix("burty-sched-");
    // 종료 시 진행 중인 배치를 끊지 않는다. 이체 정산 도중 끊기면 미결 건이 남는다.
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setErrorHandler(
        t ->
            org.slf4j.LoggerFactory.getLogger(ScheduleConfig.class)
                .error("스케줄 작업에서 처리되지 않은 예외 발생", t));
    return scheduler;
  }

  /** {@code @Async} 전용 실행기. 스케줄러 풀과 공유하면 배치가 비동기 작업을 굶긴다. */
  @Bean("applicationTaskExecutor")
  public Executor applicationTaskExecutor(
      @org.springframework.beans.factory.annotation.Value("${burty.async.core-pool-size:8}")
          int corePoolSize,
      @org.springframework.beans.factory.annotation.Value("${burty.async.max-pool-size:32}")
          int maxPoolSize,
      @org.springframework.beans.factory.annotation.Value("${burty.async.queue-capacity:500}")
          int queueCapacity) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("burty-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    // 큐가 가득 차면 조용히 버리지 않고 호출 스레드에서 실행한다 (역압).
    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
