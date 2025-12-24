package com.bank.kb.repo;

import com.bank.kb.entity.KnowledgeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeRecordRepo extends JpaRepository<KnowledgeRecord, Long>, JpaSpecificationExecutor<KnowledgeRecord> {


    @Query(value = "select * from knowledge_record order by id desc limit :n", nativeQuery = true)
    List<KnowledgeRecord> findTopN(@Param("n") int n);


}
