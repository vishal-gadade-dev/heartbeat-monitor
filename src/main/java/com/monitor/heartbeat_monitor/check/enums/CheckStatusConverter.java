package com.monitor.heartbeat_monitor.check.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CheckStatusConverter implements AttributeConverter<CheckStatus, String> {

    @Override
    public String convertToDatabaseColumn(CheckStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public CheckStatus convertToEntityAttribute(String dbData) {
        return CheckStatus.fromString(dbData);
    }
}
