package com.ktb.marong.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@Profile("local")
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.directory}")
    private String uploadDir;

    @Value("${file.upload.url.prefix}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트 디렉토리 기준 절대 경로로 리소스 위치 설정
        String resourcePath = System.getProperty("user.dir") + File.separator + uploadDir;

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + resourcePath);
    }
}