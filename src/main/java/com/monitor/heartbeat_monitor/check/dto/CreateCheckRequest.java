package com.monitor.heartbeat_monitor.check.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckRequest {

    @NotBlank(message = "Check name is required")
    private String name;

    @NotBlank(message = "Kind must be 'heartbeat' or 'website'")
    private String kind;

    private String scheduleType;
    private String scheduleValue;


    @Pattern(regexp = "^(https?://.+)?$", message = "targetUrl must be a valid http or https URL")
    private String targetUrl;


    @Min(value = 1, message = "websiteIntervalSeconds must be at least 1")
    private Integer websiteIntervalSeconds;


    @PositiveOrZero(message = "gracePeriodSeconds must be 0 or greater")
    private Integer gracePeriodSeconds;

    @NotBlank(message = "Alert URL is required")
    @Pattern(regexp = "^https?://.+", message = "alertUrl must be a valid http or https URL")
    private String alertUrl;
}
