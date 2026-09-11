package com.d5wq.burger.order.entity;

import com.d5wq.burger.product.entity.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 주문 시점의 상품 단가(스냅샷). */
    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemOption> options = new ArrayList<>();

    private OrderItem(Product product, int quantity) {
        this.product = product;
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public static OrderItem of(Product product, int quantity) {
        return new OrderItem(product, quantity);
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public OrderItem addOption(OrderItemOption option) {
        option.assignOrderItem(this);
        this.options.add(option);
        return this;
    }

    /** 버거 1개에 붙는 옵션 추가금 합계(패티 추가/세트 변경 등). */
    public int optionsExtraPerUnit() {
        return options.stream().mapToInt(OrderItemOption::extraTotal).sum();
    }

    /** 라인 합계 = (기본 단가 + 옵션 추가금) x 수량. */
    public int lineTotal() {
        return (unitPrice + optionsExtraPerUnit()) * quantity;
    }
}
