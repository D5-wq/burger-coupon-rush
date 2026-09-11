package com.d5wq.burger.product.entity;

import com.d5wq.burger.common.entity.BaseTimeEntity;
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
 * 옵션 그룹 안의 선택지. (예: "베이컨 추가 +1,500", "양상추", "감자튀김", "코카콜라 L")
 */
@Entity
@Getter
@Table(name = "option_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private OptionGroup optionGroup;

    @Column(nullable = false, length = 100)
    private String name;

    /** 추가 금액(원). 기본 포함/무료 항목은 0. */
    @Column(nullable = false)
    private int extraPrice;

    /** 기본 포함 수량. 기본으로 들어가는 재료(양파/상추)는 1, 추가 옵션은 0. */
    @Column(nullable = false)
    private int defaultQuantity;

    /** 선택 가능한 최대 수량. 단일 항목은 1, 패티 추가처럼 여러 개면 2~3. */
    @Column(nullable = false)
    private int maxQuantity;

    @Column(nullable = false)
    private int displayOrder;

    private OptionItem(String name, int extraPrice, int defaultQuantity, int maxQuantity, int displayOrder) {
        this.name = name;
        this.extraPrice = extraPrice;
        this.defaultQuantity = defaultQuantity;
        this.maxQuantity = maxQuantity;
        this.displayOrder = displayOrder;
    }

    public static OptionItem of(String name, int extraPrice, int defaultQuantity, int maxQuantity, int displayOrder) {
        return new OptionItem(name, extraPrice, defaultQuantity, maxQuantity, displayOrder);
    }

    /** 단일 선택(수량 1 고정) 항목 편의 생성자. */
    public static OptionItem single(String name, int extraPrice, int displayOrder) {
        return new OptionItem(name, extraPrice, 0, 1, displayOrder);
    }

    void assignGroup(OptionGroup group) {
        this.optionGroup = group;
    }
}
