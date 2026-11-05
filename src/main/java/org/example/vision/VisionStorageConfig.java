package org.example.vision;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class VisionStorageConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 모자이크 폴더만 공개 (uploads 전체·원본 폴더는 서빙하지 않음)
        String location = Path.of("uploads", "vision", "mosaic").toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";  // 폴더가 아직 없으면 toUri()가 끝 슬래시를 붙이지 않는다
        }
        registry.addResourceHandler("/files/vision/mosaic/**")
                .addResourceLocations(location);
    }
}
