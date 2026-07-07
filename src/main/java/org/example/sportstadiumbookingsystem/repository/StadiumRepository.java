package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StadiumRepository extends JpaRepository<Stadium, Long>, JpaSpecificationExecutor<Stadium> {
    List<Stadium> findByOwnerId(Long ownerId);
    List<Stadium> findByStatus(StadiumStatus status);
    long countByStatus(StadiumStatus status);
}