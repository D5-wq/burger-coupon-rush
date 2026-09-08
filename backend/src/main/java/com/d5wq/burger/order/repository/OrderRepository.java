package com.d5wq.burger.order.repository;

import com.d5wq.burger.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByIdDesc(Long userId);

    @Query("select o from Order o join fetch o.user where o.id = :id")
    Optional<Order> findWithUserById(Long id);
}
