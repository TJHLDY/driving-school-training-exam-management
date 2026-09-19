package com.example.drivingschool.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    public static final DateTimeFormatter API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer apiJacksonCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            });
            builder.serializerByType(java.time.LocalDateTime.class, new LocalDateTimeSerializer(API_DATE_TIME));
            builder.deserializerByType(java.time.LocalDateTime.class, new LocalDateTimeDeserializer(API_DATE_TIME));
        };
    }
}
