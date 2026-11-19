package com.parquesoft.ecommerce.repository;

import com.parquesoft.ecommerce.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmail(String email);

    boolean existsByEmail(String email);


    List<CustomerEntity> findByIsActiveTrue();

    @Query("SELECT c FROM CustomerEntity c WHERE c.loyaltyPoints >= :minPoints AND c.isActive = true ORDER BY c.loyaltyPoints DESC")
    List<CustomerEntity> findVipCustomers(@Param("minPoints") int minPoints);
}
