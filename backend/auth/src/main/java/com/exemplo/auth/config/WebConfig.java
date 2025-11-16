// src/main/java/com/exemplo/auth/config/WebConfig.java
package com.exemplo.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // caminho absoluto do diretório "data"
        String dataRoot = Paths.get("data").toAbsolutePath().toString().replace("\\", "/");
        // Qualquer URL que começar com /files/ será servida a partir do disco em /data/...
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + dataRoot + "/");
    }
}
