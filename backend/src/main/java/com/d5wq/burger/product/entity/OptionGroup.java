package com.d5wq.burger.product.entity;

import com.d5wq.burger.common.entity.BaseTimeEntity;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품(버거)에 붙는 옵션 묶음. (예: "구성(단품/세트)", "재료 추가", "사이드", "음료")
 * 하나의 상품이 여러 그룹을 가진다.
 */
@Entity
@Getter
@Table(name = "option_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 옵션 그룹이 속한 상품. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private com.d5wq.burger.product.entity.Product product;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SelectionType selectionType;

    /** 필수 선택 여부. */
    @Column(nullable = false)
    private boolean required;

    /** 최소/최대 선택 개수 (MULTI에서 의미, SINGLE은 보통 1/1). */
    @Column(nullable = false)
    private int minSelect;

    @Column(nullable = false)
    private int maxSelect;

    @Column(nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "optionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<OptionItem> items = new ArrayList<>();

    private OptionGroup(Product product, String name, SelectionType selectionType,
            boolean required, int minSelect, int maxSelect, int displayOrder) {
        this.product = product;
        this.name = name;
        this.selectionType = selectionType;
        this.required = required;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
        this.displayOrder = displayOrder;
    }

    public static OptionGroup of(Product product, String name, SelectionType selectionType,
            boolean required, int minSelect, int maxSelect, int displayOrder) {
        return new OptionGroup(product, name, selectionType, required, minSelect, maxSelect, displayOrder);
    }

    public OptionGroup addItem(OptionItem item) {
        item.assignGroup(this);
        this.items.add(item);
        return this;
    }
}
