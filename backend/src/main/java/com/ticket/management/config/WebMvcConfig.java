package com.ticket.management.config;

import com.ticket.management.security.ApiPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiPermissionInterceptor apiPermissionInterceptor;

    private static final String[] EXCLUDE_PATHS = {
        "/api/auth/**",
        "/api/public/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/actuator/**",
        "/error/**"
    };

    @Autowired
    public WebMvcConfig(ApiPermissionInterceptor apiPermissionInterceptor) {
        this.apiPermissionInterceptor = apiPermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiPermissionInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(EXCLUDE_PATHS)
            .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
