package com.guideon.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Async 전용 스레드 풀 설정.
 *
 * FastApiDocumentService.processDocument()의 @Async 호출에 사용됨.
 * Spring 기본 SimpleAsyncTaskExecutor는 요청마다 스레드를 무제한 생성하므로
 * 문서 업로드 burst 시 스레드 폭증을 방지하기 위해 풀을 제한.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "fastApiTaskExecutor")
    public Executor fastApiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);       // 평소 유지 스레드 수
        executor.setMaxPoolSize(10);       // 최대 스레드 수
        executor.setQueueCapacity(50);     // 대기 큐 (초과 시 reject)
        executor.setThreadNamePrefix("fastapi-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
