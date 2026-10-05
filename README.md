# Learn Spring Cloud

Notes & sample projects while learning Spring Cloud.

## Các project trong repo

| Service | Port | Vai trò |
|---|---|---|
| `ecom-order-service` | 8080 | Đặt hàng, gọi sang inventory-service |
| `ecom-inventory-service` | 8081 | Quản lý tồn kho (H2 + JPA) |

`ecom-order-service` hiện đang demo cả **RestTemplate** và **RestClient**:
- [RestTemplateConfig.java](./ecom-order-service/src/main/java/org/codesnippet/ecomorderservice/config/RestTemplateConfig.java)
- [RestClientConfig.java](./ecom-order-service/src/main/java/org/codesnippet/ecomorderservice/config/RestClientConfig.java)
- [OrderService.java](./ecom-order-service/src/main/java/org/codesnippet/ecomorderservice/services/OrderService.java)

## So sánh HTTP client: RestTemplate vs RestClient vs OpenFeign

Cả 3 đều là **blocking (synchronous)** client. Chọn cái nào phụ thuộc vào độ phức tạp của hệ thống.

### Bảng so sánh tổng quan

| Tiêu chí | RestTemplate | RestClient | OpenFeign |
|---|---|---|---|
| Ra mắt | Spring 3.0 (2009) | Spring 6.1 / Boot 3.2 (2023) | Spring Cloud OpenFeign |
| Trạng thái | 🔸 Maintenance mode (từ Spring 5) | ✅ Hiện đại, được khuyến nghị | 🔸 Feature-complete, ít phát triển thêm |
| Kiểu API | Imperative, method-per-verb (`getForObject`, `postForEntity`) | Fluent / builder (`.get().uri().retrieve()`) | Declarative (interface + annotation) |
| Boilerplate | Trung bình | Thấp | Rất thấp |
| Service discovery | ❌ Không tự động | ❌ Không tự động | ✅ Tích hợp sẵn (Eureka / Consul) |
| Load balancing | ❌ | ❌ | ✅ Spring Cloud LoadBalancer |
| Retry / Circuit breaker | ❌ Tự làm | ❌ Tự làm | ✅ `Retryer` / Resilience4j |
| Error handling | `ResponseErrorHandler` | `.onStatus(...)` | `ErrorDecoder` |
| Testing | Mock `RestTemplate` | `MockRestServiceServer`, mock request factory | WireMock / `@SpringBootTest` |
| Dependency | Có sẵn trong spring-web | Có sẵn trong spring-web | `spring-cloud-starter-openfeign` + Spring Cloud BOM |
| Phù hợp | Code cũ, gọi đơn giản | Dự án mới, cần kiểm soát chi tiết | Microservices, nhiều service, có discovery |

### Ví dụ code

**1. RestTemplate** (classic, imperative)

```java
@Bean
RestTemplate restTemplate() {
    return new RestTemplate();
}

String body = restTemplate.getForObject(
        "http://localhost:8081/inventory/{productId}",
        String.class, productId);
```

**2. RestClient** (fluent, thay thế RestTemplate)

```java
@Bean
RestClient restClient() {
    return RestClient.create();
}

ResponseEntity<Inventory> entity = restClient.get()
        .uri("http://localhost:8081/inventory/{productId}", productId)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
            throw new MyCustomRuntimeException(response.getStatusCode(), response.getHeaders());
        })
        .toEntity(Inventory.class);
```

**3. OpenFeign** (declarative — chỉ cần interface)

```java
@FeignClient(name = "inventory-service", url = "http://localhost:8081")
public interface InventoryClient {

    @GetMapping("/inventory/{productId}")
    Inventory getInventory(@PathVariable Long productId);
}
```

```java
@SpringBootApplication
@EnableFeignClients   // bật Feign
public class EcomOrderServiceApplication { /* ... */ }
```

### Trade-offs chi tiết

**RestTemplate**
- ➕ Quen thuộc, tài liệu nhiều, ổn định, không cần thêm dependency.
- ➖ API cũ (method-per-verb), dễ rối khi cấu hình nâng cao; đang ở *maintenance mode*; khó test hơn RestClient.
- 👉 Dùng khi: giữ code cũ, hoặc các call rất đơn giản.

**RestClient**
- ➕ Fluent API dễ đọc; hỗ trợ `retrieve()` / `exchange()` / `onStatus()`; dùng chung request factory với RestTemplate (JDK, Apache, Jetty...); là hướng đi chính thức của Spring cho sync client; test dễ.
- ➖ Vẫn là imperative → còn boilerplate khi có nhiều endpoint; **không** tích hợp discovery / load balancing / circuit breaker; phải tự viết wrapper cho từng service.
- 👉 Dùng khi: dự án mới, muốn sync, không cần service discovery, hoặc cần kiểm soát request/response ở mức thấp.

**OpenFeign**
- ➕ Declarative — chỉ khai báo interface, gần như không boilerplate; tích hợp sẵn service discovery, load balancing, retry, circuit breaker; code gọn, dễ đọc.
- ➖ Kéo thêm dependency + BOM Spring Cloud; "magic" qua proxy/reflection nên khó debug và lỗi chỉ lộ lúc runtime; không dùng được trong luồng WebFlux (vẫn blocking); được xem là *feature-complete*; cấu hình timeout/error phức tạp hơn.
- 👉 Dùng khi: hệ microservices nhiều service, có discovery (Eureka/Consul), cần load balancing & resilience.

### Lưu ý về blocking vs reactive

Cả RestTemplate, RestClient và OpenFeign đều **blocking**. Nếu cần **non-blocking / reactive**, dùng `WebClient` (Spring WebFlux) — nhưng khi đó toàn bộ luồng nên theo reactive để tránh block thread.

### Khuyến nghị

| Tình huống | Lựa chọn |
|---|---|
| Dự án mới, thuần Spring Boot, gọi vài service | **RestClient** |
| Code cũ đang dùng RestTemplate | Giữ **RestTemplate**, migrate dần sang RestClient |
| Microservices nhiều service, có discovery & cần resilience | **OpenFeign** |
| Cần non-blocking | **WebClient** |

> Xu hướng hiện tại: `RestTemplate → RestClient`, và `OpenFeign → Declarative HTTP Service Clients` (Spring Boot 4 / Spring Framework 7 với `@ImportHttpServices`, không cần Spring Cloud).
