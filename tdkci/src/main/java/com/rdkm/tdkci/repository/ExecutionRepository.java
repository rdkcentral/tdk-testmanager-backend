package com.rdkm.tdkci.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkci.model.Execution;

/**
 * Repository interface for managing Execution entities.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

}
