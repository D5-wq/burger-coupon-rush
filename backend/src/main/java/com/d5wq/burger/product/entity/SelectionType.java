package com.d5wq.burger.product.entity;

/**
 * 옵션 그룹의 선택 방식.
 * - SINGLE: 하나만 선택 (예: 구성=단품/세트, 사이드, 음료)
 * - MULTI: 여러 개 선택 + 항목별 수량 (예: 재료 추가/제거)
 */
public enum SelectionType {
    SINGLE,
    MULTI
}
