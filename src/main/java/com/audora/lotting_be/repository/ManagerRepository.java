// src/main/java/com/audora/lotting_be/repository/ManagerRepository.java
package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.manager.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByUsername(String username);
    Boolean existsByUsername(String username);
}
