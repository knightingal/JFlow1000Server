package org.nanking.knightingal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Knightingal
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final static boolean STATIC_DIR = false;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (STATIC_DIR) {
            registry.addResourceHandler("/static/**").addResourceLocations("file:/home/knightingal/download/linux1000/");
            registry.addResourceHandler("/dist/**").addResourceLocations("classpath:static/frontEnd/dist/");
            registry.addResourceHandler("/lib/**").addResourceLocations("classpath:static/frontEnd/node_modules/");
            registry.addResourceHandler("/frontEnd/**").addResourceLocations("classpath:static/frontEnd/");
        }
    }

    @Autowired
    Flow1000Interceptor flow1000Interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(flow1000Interceptor).addPathPatterns("/flow1000/**");
    }
}
