package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByDocTypeAndNaturalKey(String docType, String naturalKey);
}
