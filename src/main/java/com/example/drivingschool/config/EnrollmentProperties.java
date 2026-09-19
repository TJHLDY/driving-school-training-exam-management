package com.example.drivingschool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.enrollment")
public record EnrollmentProperties(BigDecimal c1Amount, BigDecimal c2Amount, BigDecimal plannedHours) {

    public EnrollmentProperties {
        if (c1Amount == null || c1Amount.signum() <= 0) {
            c1Amount = new BigDecimal("4800.00");
        }
        if (c2Amount == null || c2Amount.signum() <= 0) {
            c2Amount = new BigDecimal("5000.00");
        }
        if (plannedHours == null || plannedHours.signum() <= 0) {
            plannedHours = new BigDecimal("40.00");
        }
    }

    public BigDecimal defaultAmount(String licenseType) {
        return "C2".equals(licenseType) ? c2Amount : c1Amount;
    }
}
