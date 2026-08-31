package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
SELECT u FROM User u WHERE
(:id IS NULL OR u.id = :id) AND
(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND
(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND
(:middleName IS NULL OR LOWER(u.middleName) LIKE LOWER(CONCAT('%', :middleName, '%'))) AND
(:role IS NULL OR u.role = :role)
""")
    Page<User> findAllWithFilter(
            @Param("id") Long id,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("middleName") String middleName,
            @Param("role") Role role,
            Pageable pageable);
    Optional<User> findByEmail(String email);

    void deleteByEmail(String email);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
