package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.StadiumImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StadiumImageRepository extends JpaRepository<StadiumImage, Long> {
    List<StadiumImage> findByStadiumId(Long stadiumId);
    List<StadiumImage> findByStadiumIdOrderByIsPrimaryDescCreatedAtAsc(Long stadiumId);
    Optional<StadiumImage> findByStadiumIdAndIsPrimaryTrue(Long stadiumId);
    long countByStadiumId(Long stadiumId);
    void deleteByStadiumId(Long stadiumId);
}