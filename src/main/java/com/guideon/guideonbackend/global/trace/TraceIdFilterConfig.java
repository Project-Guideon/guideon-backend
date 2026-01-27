package com.guideon.guideonbackend.global.trace;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceIdFilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new TraceIdFilter());
        // 실행 순서 설정: 1 , 먼저실행하기 위함
        bean.setOrder(1);
        bean.addUrlPatterns("/*");

        return bean;
    }
}
