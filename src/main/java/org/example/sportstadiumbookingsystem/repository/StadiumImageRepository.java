package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.StadiumImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StadiumImageRepository extends JpaRepository<StadiumImage, Long> {
    List<StadiumImage> findByStadiumId(Long stadiumId);
}
