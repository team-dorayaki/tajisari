package com.tajisali.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            builder.withCoercionConfig(
                    LogicalType.Enum,
                    config -> config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail));
        };
    }
}
