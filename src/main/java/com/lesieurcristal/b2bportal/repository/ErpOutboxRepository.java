package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.ErpOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpOutboxRepository extends JpaRepository<ErpOutbox, Long> {

    List<ErpOutbox> findTop20ByDirectionAndStatusInOrderByCreatedAtAsc(
            String direction, List<String> statuses);

    List<ErpOutbox> findByDirectionAndEventTypeOrderByIdAsc(String direction, String eventType);
}
