package com.d5wq.burger.order.entity;

import com.d5wq.burger.common.entity.BaseTimeEntity;
import com.d5wq.burger.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    /** 할인 전 합계. */
    @Column(nullable = false)
    private int totalPrice;

    /** 쿠폰 등으로 할인된 금액(Step 5에서 쿠폰 적용 연동). */
    @Column(nullable = false)
    private int discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    private Order(User user) {
        this.user = user;
        this.status = OrderStatus.CREATED;
        this.discountAmount = 0;
    }

    public static Order create(User user, List<OrderItem> items) {
        Order order = new Order(user);
        for (OrderItem item : items) {
            order.addItem(item);
        }
        return order;
    }

    private void addItem(OrderItem item) {
        item.assignOrder(this);
        this.orderItems.add(item);
        this.totalPrice += item.lineTotal();
    }

    public void applyDiscount(int discountAmount) {
        this.discountAmount = Math.min(discountAmount, this.totalPrice);
    }

    public int getFinalPrice() {
        return totalPrice - discountAmount;
    }
}
