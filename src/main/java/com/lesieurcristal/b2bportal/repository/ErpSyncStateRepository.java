package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.ErpSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErpSyncStateRepository extends JpaRepository<ErpSyncState, String> {
}
