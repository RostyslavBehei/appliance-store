package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE (:keyword IS NULL OR o.id = :keyword)")
    Page<Order> findAllWithFilter(
            @Param("keyword") String keyword,
            Pageable pageable);
    Page<Order> findByClientEmail(String clientEmail, Pageable pageable);

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.approved = true AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long calculateTotalRevenue(
            @Param("startDate")LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByApproved(boolean value);
}
