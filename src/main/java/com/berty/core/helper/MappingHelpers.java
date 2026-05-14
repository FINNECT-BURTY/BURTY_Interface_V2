package com.berty.core.helper;

import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MappingHelpers {

    @Named("formatDateTime")
    public String formatDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Named("enumName")
    public String enumName(Enum<?> e) {
        return e == null ? null : e.name();
    }
}
