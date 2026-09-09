package com.d5wq.burger.common.init;

import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 상품(버거 메뉴)이 비어 있으면 시드 데이터를 넣는다.
 * 멱등하게 동작하도록 이미 데이터가 있으면 건너뛴다.
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("[seed] products already exist, skip");
            return;
        }
        List<Product> burgers = List.of(
                burger("클래식 치즈버거", 6500,
                        "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=70",
                        "두툼한 소고기 패티에 체다 치즈, 신선한 양상추와 토마토"),
                burger("더블 베이컨 버거", 8900,
                        "https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=500&q=70",
                        "패티 두 장과 바삭한 베이컨, 스모키한 BBQ 소스"),
                burger("스모키 불고기 버거", 7200,
                        "https://images.unsplash.com/photo-1550547660-d9450f859349?w=500&q=70",
                        "한국식 불고기 양념 패티와 구운 양파"),
                burger("스파이시 치킨 버거", 6900,
                        "https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=500&q=70",
                        "바삭한 통닭다리살에 매콤한 핫소스"),
                burger("머쉬룸 스위스 버거", 7800,
                        "https://images.unsplash.com/photo-1572802419224-296b0aeee0d9?w=500&q=70",
                        "볶은 양송이버섯과 스위스 치즈의 진한 풍미"),
                burger("아보카도 비프 버거", 8500,
                        "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=500&q=70",
                        "부드러운 아보카도와 소고기 패티의 조화"),
                burger("트러플 마요 버거", 9500,
                        "https://images.unsplash.com/photo-1610440042657-612c34d95e9f?w=500&q=70",
                        "트러플 마요네즈와 카라멜라이즈드 어니언"),
                burger("클래식 비프 버거", 5900,
                        "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?w=500&q=70",
                        "기본에 충실한 소고기 패티 단품 버거")
        );
        productRepository.saveAll(burgers);
        log.info("[seed] inserted {} products", burgers.size());
    }

    private Product burger(String name, int price, String imageUrl, String description) {
        return Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .description(description)
                .build();
    }
}
