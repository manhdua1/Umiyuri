package com.pandora.coreservice.repo;

import com.pandora.coreservice.entity.Department;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Department> findActiveByIdForReference(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Department> findActiveByIdForUpdate(@Param("id") UUID id);

    Page<Department> findByDeletedAtIsNull(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Department d SET d.manager = null WHERE d.manager.id = :managerId")
    int clearManagerByManagerId(@Param("managerId") UUID managerId);
}
