package com.backend.security.profile;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<HostBasedProfileFilter> customHostBasedProfileFilter() {
        FilterRegistrationBean<HostBasedProfileFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HostBasedProfileFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}