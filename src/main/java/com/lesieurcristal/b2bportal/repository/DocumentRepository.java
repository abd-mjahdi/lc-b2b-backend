package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT d FROM Document d JOIN FETCH d.customer "
            + "WHERE d.docType = :docType AND d.naturalKey = :naturalKey")
    Optional<Document> findByDocTypeAndNaturalKey(
            @Param("docType") String docType,
            @Param("naturalKey") String naturalKey);
}
