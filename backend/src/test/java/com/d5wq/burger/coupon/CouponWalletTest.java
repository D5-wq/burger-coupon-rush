package com.d5wq.burger.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.d5wq.burger.coupon.dto.CouponCreateRequest;
import com.d5wq.burger.coupon.dto.MyCouponResponse;
import com.d5wq.burger.coupon.entity.ApplyScope;
import com.d5wq.burger.coupon.service.CouponIssueService;
import com.d5wq.burger.coupon.service.IssueStrategy;
import com.d5wq.burger.user.dto.SignUpRequest;
import com.d5wq.burger.user.dto.UserResponse;
import com.d5wq.burger.user.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 내 쿠폰함 조회. 특히 ORDER 범위 쿠폰(productId=null)만 보유한 경우에도
 * 상품명 조회에서 NPE 없이 동작해야 한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class CouponWalletTest {

    @Autowired
    AuthService authService;
    @Autowired
    CouponIssueService couponIssueService;

    @Test
    @DisplayName("ORDER 범위 쿠폰만 보유해도 쿠폰함 조회가 성공한다(productId=null)")
    void myCoupons_withOnlyOrderScope_doesNotThrow() {
        UserResponse user = authService.signUp(new SignUpRequest("wallet-order@test.com", "pass1234", "지갑"));
        Long couponId = couponIssueService.createCoupon(
                new CouponCreateRequest("전체 10%", 10, ApplyScope.ORDER, null, 100, null, null));
        couponIssueService.issue(couponId, user.id(), IssueStrategy.NAIVE);

        List<MyCouponResponse> mine = couponIssueService.getMyCoupons(user.id());

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).applyScope()).isEqualTo("ORDER");
        assertThat(mine.get(0).productId()).isNull();
        assertThat(mine.get(0).productName()).isNull();
        assertThat(mine.get(0).used()).isFalse();
    }
}
