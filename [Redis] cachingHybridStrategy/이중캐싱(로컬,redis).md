
# 이중 캐싱의 성능 우위와 실제 서비스 구현 방법

## 1. 왜 이중 캐싱이 더 빠른가?

이중 캐싱이 단일 캐싱(Redis만 사용)보다 빠른 이유는 **접근 속도의 계층적 차이** 때문입니다:

1. **접근 속도 차이**:
   - 로컬 메모리 접근: 약 50-100 나노초
   - Redis 접근: 약 0.1-1 밀리초 (로컬 메모리보다 10-20배 느림)
   - 데이터베이스 접근: 약 10-100 밀리초 (Redis보다 10-100배 느림)

2. **네트워크 오버헤드 제거**:
   - Redis는 네트워크 통신이 필요한 반면, 로컬 캐시는 같은 프로세스 내 메모리에서 즉시 응답
   - 작업 당 1ms의 네트워크 지연만 있어도 수천 요청에서는 수 초의 차이가 발생

3. **테스트 코드에서 확인할 수 있는 예**:
   ```java
   // 로컬 캐시 히트 경우: 매우 빠름 (네트워크 통신 없음)
   Product product = localCache.get(id); // ~0.1ms

   // Redis 캐시 히트 경우: 상대적으로 느림 (네트워크 통신 필요)
   product = cacheAsideService.getProduct(id); // ~1-2ms
   ```

## 2. 실제 서비스에서 로컬 캐싱 구현 방법

테스트에서는 간단한 `ConcurrentHashMap`을 사용했지만, 실제 서비스에서는 더 체계적인 접근이 필요합니다:

### 1) 적절한 캐시 라이브러리 선택

```java
// 단순 HashMap 대신 Caffeine 캐시 라이브러리 사용 예
Cache<Long, Product> localCache = Caffeine.newBuilder()
    .maximumSize(10_000)       // 최대 항목 수 제한
    .expireAfterWrite(5, TimeUnit.MINUTES)  // 만료 시간 설정
    .recordStats()             // 통계 기록
    .build();
```

**권장 라이브러리**:
- **Caffeine**: 고성능 자바 캐시 라이브러리 (Spring Boot 2.x 이상의 기본 캐시)
- **Guava Cache**: Google의 캐시 라이브러리
- **Ehcache**: 디스크 저장도 지원하는 더 큰 규모의 캐시

### 2) 메모리 관리 및 캐시 정책 설정

```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .weakKeys()
            .recordStats());
        return cacheManager;
    }
}
```

**주요 정책**:
- **크기 제한**: 메모리 사용량 제한
- **만료 정책**: 시간 기반(TTL), 접근 기반(LRU), 사용 빈도 기반(LFU)
- **제거 알림**: 캐시 항목 제거 시 콜백 함수 실행

### 3) 분산 환경에서의 일관성 관리

여러 서버 인스턴스가 있는 경우 로컬 캐시 일관성 문제를 해결해야 합니다:

```java
@Service
public class ProductService {
    private final Cache<Long, Product> localCache;
    private final RedisCacheManager redisCacheManager;
    private final RedisMessageListenerContainer messageListener;
    
    // 다른 서버에서 캐시 무효화 메시지를 수신하는 리스너 설정
    public ProductService() {
        // ... 초기화 코드 ...
        messageListener.addMessageListener((message) -> {
            String productId = new String(message.getBody());
            localCache.invalidate(Long.parseLong(productId));
        }, new ChannelTopic("cache:invalidate:product"));
    }
    
    // 상품 업데이트 시 캐시 무효화 메시지 발행
    public void updateProduct(Product product) {
        // DB 업데이트
        productRepository.save(product);
        
        // Redis 캐시 갱신
        redisCacheManager.getCache("products").put(product.getId(), product);
        
        // 로컬 캐시 갱신
        localCache.put(product.getId(), product);
        
        // 다른 서버에 무효화 메시지 발행
        redisTemplate.convertAndSend("cache:invalidate:product", 
                                     product.getId().toString());
    }
}
```

### 4) 캐시 계층 구현 패턴

실제 서비스에서 사용할 수 있는 이중 캐싱 패턴:

```java
public Product getProductWithMultiLevelCache(Long id) {
    // 1. 로컬 캐시 확인
    Product product = localCache.getIfPresent(id);
    if (product != null) {
        cacheMetrics.incrementLocalHit();
        return product;
    }
    
    // 2. Redis 캐시 확인
    String cacheKey = "product:" + id;
    product = (Product) redisTemplate.opsForValue().get(cacheKey);
    if (product != null) {
        cacheMetrics.incrementRedisHit();
        // 발견한 결과를 로컬 캐시에도 저장
        localCache.put(id, product);
        return product;
    }
    
    // 3. DB 조회
    cacheMetrics.incrementDbHit();
    product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    
    // 4. 양쪽 캐시에 저장
    redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
    localCache.put(id, product);
    
    return product;
}
```

### 5) 구체적인 활용 시나리오

**마이크로서비스 환경의 상품 서비스**:
- **높은 읽기 부하**: 카탈로그 상품은 읽기가 빈번하고 수정은 드물어 로컬 캐싱에 이상적
- **계층별 TTL 차등화**: 로컬 캐시(1분), Redis 캐시(10분), 두 계층의 수명을 다르게 설정
- **선택적 캐싱**: 인기 상품만 로컬 캐시에 보관(접근 빈도 추적)

**로컬 캐시 적용 사례**:
- **상품 카탈로그**: 자주 조회되지만 드물게 변경되는 상품 정보
- **사용자 프로필**: 세션 동안 자주 접근되는 기본 정보
- **설정/구성 데이터**: 애플리케이션 설정, 시스템 상수 등 거의 변경되지 않는 정보
- **자주 사용되는 계산 결과**: 비용이 많이 드는 연산 결과

### 6) 모니터링 및 미세 조정

```java
@Scheduled(fixedRate = 60000)  // 1분마다 실행
public void reportCacheMetrics() {
    CacheStats stats = localCache.stats();
    log.info("캐시 성능: 히트율={}%, 평균 로딩 시간={}ns, 제거 수={}",
             stats.hitRate() * 100,
             stats.averageLoadPenalty(),
             stats.evictionCount());
    
    // 캐시 크기나 정책 동적 조정 
    if (stats.hitRate() < 0.5) {  // 히트율이 50% 미만이면
        // 캐시 크기 증가 또는 정책 조정
        reconfigureCache();
    }
}
```

이중 캐싱은 적절히 구성하면 성능을 크게 향상시킬 수 있지만, 캐시 일관성과 메모리 사용량 관리가 중요합니다. 각 서비스의 특성에 맞는 캐시 정책과 구현 방식을 선택해야 합니다.
