package org.example.vision;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class VisionStorageConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of("uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}
