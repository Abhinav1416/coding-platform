package com.Abhinav.backend.features.authentication.configuration;

import com.Abhinav.backend.features.authentication.filter.AuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AuthenticationConfiguration {
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> customAuthenticationFilter(AuthenticationFilter filter) {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}