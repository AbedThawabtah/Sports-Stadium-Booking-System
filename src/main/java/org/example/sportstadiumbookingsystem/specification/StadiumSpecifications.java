package org.example.sportstadiumbookingsystem.specification;

import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class StadiumSpecifications {

    private StadiumSpecifications() {
    }

    public static Specification<Stadium> hasStatus(StadiumStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Stadium> hasCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    public static Specification<Stadium> hasSportType(String sportType) {
        if (sportType == null || sportType.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("sportType")), sportType.toLowerCase());
    }

    public static Specification<Stadium> hasMinPrice(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pricePerHour"), minPrice);
    }

    public static Specification<Stadium> hasMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("pricePerHour"), maxPrice);
    }

    public static Specification<Stadium> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("location")), pattern),
                cb.like(cb.lower(root.get("city")), pattern)
        );
    }

    public static Specification<Stadium> hasAvailableSlotOn(LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var slot = subquery.from(TimeSlot.class);
            subquery.select(slot.get("id"))
                    .where(
                            cb.equal(slot.get("stadium"), root),
                            cb.equal(slot.get("slotDate"), date),
                            cb.equal(slot.get("status"), SlotStatus.AVAILABLE)
                    );
            return cb.exists(subquery);
        };
    }
}
