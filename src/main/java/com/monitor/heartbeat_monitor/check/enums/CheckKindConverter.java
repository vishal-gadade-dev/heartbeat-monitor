package com.monitor.heartbeat_monitor.check.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CheckKindConverter implements AttributeConverter<CheckKind, String> {

    @Override
    public String convertToDatabaseColumn(CheckKind attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public CheckKind convertToEntityAttribute(String dbData) {
        return CheckKind.fromString(dbData);
    }
}
