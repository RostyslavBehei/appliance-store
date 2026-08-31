package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApplianceRepository extends JpaRepository<Appliance, Long> {

    Optional<Appliance> findByModel(String model);

    Page<Appliance> findByCategory(Category category, Pageable pageable);
    Page<Appliance> findByManufacturerName(String manufacturerName, Pageable pageable);

    @Query("""
SELECT a FROM Appliance a WHERE
(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND
(:category IS NULL OR a.category = :category) AND
(:manufacturerName IS NULL OR LOWER(a.manufacturer.name) LIKE LOWER(CONCAT('%', :manufacturerName, '%'))) AND
(:powerType IS NULL OR a.powerType = :powerType) AND
(:power IS NULL OR a.power = :power) AND
(:minPrice IS NULL OR a.price >= :minPrice) AND
(:maxPrice IS NULL OR a.price <= :maxPrice)
""")
    Page<Appliance> findAllWithFilter(
            @Param("name") String name,
            @Param("category") Category category,
            @Param("manufacturerName") String manufacturerName,
            @Param("powerType") PowerType powerType,
            @Param("power") Integer power,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    long countByCategory(Category category);
}
