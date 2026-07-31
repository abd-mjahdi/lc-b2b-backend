package com.lesieurcristal.b2bportal.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientChangeLogDTO {
    private Long id;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private OffsetDateTime changedAt;
    private String changedByUser;
}
