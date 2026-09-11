package com.d5wq.burger.order.entity;

import com.d5wq.burger.product.entity.OptionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 항목이 선택한 옵션의 스냅샷. (예: "재료 추가·제거 / 패티 추가 x2 / +1,500")
 * 옵션의 이름·추가금액은 주문 시점 값으로 복사해 두어, 이후 옵션 정보가 바뀌어도 과거 주문은 그대로 남는다.
 */
@Entity
@Getter
@Table(name = "order_item_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** 어떤 옵션 항목이었는지 참조용 id 스냅샷. */
    @Column(nullable = false)
    private Long optionItemId;

    /** 옵션 그룹명 스냅샷. (예: "재료 추가·제거") */
    @Column(nullable = false, length = 100)
    private String groupName;

    /** 옵션 항목명 스냅샷. (예: "패티 추가") */
    @Column(nullable = false, length = 100)
    private String itemName;

    /** 항목 1개당 추가 금액 스냅샷. */
    @Column(nullable = false)
    private int extraPrice;

    /** 선택 수량. (예: 패티 2개) */
    @Column(nullable = false)
    private int quantity;

    private OrderItemOption(Long optionItemId, String groupName, String itemName, int extraPrice, int quantity) {
        this.optionItemId = optionItemId;
        this.groupName = groupName;
        this.itemName = itemName;
        this.extraPrice = extraPrice;
        this.quantity = quantity;
    }

    /** 선택한 옵션 항목과 수량으로 스냅샷을 만든다. */
    public static OrderItemOption of(OptionItem source, int quantity) {
        return new OrderItemOption(
                source.getId(),
                source.getOptionGroup().getName(),
                source.getName(),
                source.getExtraPrice(),
                quantity);
    }

    void assignOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    /** 이 옵션이 버거 1개에 더하는 추가금(추가금 x 수량). */
    public int extraTotal() {
        return extraPrice * quantity;
    }
}
