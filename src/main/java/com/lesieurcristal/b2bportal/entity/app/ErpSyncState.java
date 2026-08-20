package com.lesieurcristal.b2bportal.entity.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "erp_sync_state", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ErpSyncState {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "last_run_at")
    private OffsetDateTime lastRunAt;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_cursor")
    private OffsetDateTime lastCursor;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "records_pulled", nullable = false)
    @Builder.Default
    private Integer recordsPulled = 0;
}
