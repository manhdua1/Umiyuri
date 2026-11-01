package com.pandora.coreservice.repo;

import com.pandora.coreservice.entity.Employee;
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
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT e FROM Employee e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findActiveByIdForReference(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Employee> findActiveByIdForUpdate(@Param("id") UUID id);

    Page<Employee> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Employee e SET e.department = null WHERE e.department.id = :departmentId")
    int clearDepartmentByDepartmentId(@Param("departmentId") UUID departmentId);
}
