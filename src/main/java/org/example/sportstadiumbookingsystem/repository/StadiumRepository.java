package org.example.sportstadiumbookingsystem.repository;


import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    List<Stadium> findByOwnerId(Long ownerId);
}
