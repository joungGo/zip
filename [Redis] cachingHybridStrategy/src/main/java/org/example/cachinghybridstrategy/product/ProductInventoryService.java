package org.example.cachinghybridstrategy.product;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductInventoryService {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "product:inventory:";
    private static final long CACHE_TTL = 3600;
    private static final long REFRESH_AHEAD_TIME = 300;

    private final Set<Long> refreshQueue = ConcurrentHashMap.newKeySet();

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return getProduct(id);
    }

    public Product createProduct(Product product) {
        product.setLastUpdated(LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        String cacheKey = CACHE_KEY_PREFIX + savedProduct.getId();
        redisTemplate.opsForValue().set(cacheKey, savedProduct, CACHE_TTL, TimeUnit.SECONDS);
        return savedProduct;
    }

    public Product updateProduct(Long id, Product product) {
        Product existingProduct = getProduct(id);
        product.setId(id);
        product.setLastUpdated(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.opsForValue().set(cacheKey, updatedProduct, CACHE_TTL, TimeUnit.SECONDS);
        return updatedProduct;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        redisTemplate.delete(CACHE_KEY_PREFIX + id);
        refreshQueue.remove(id);
    }

    @Transactional
    public Product updateStock(Long productId, int quantity) {
        // Write-Through: 재고 업데이트 즉시 반영
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setStock(product.getStock() - quantity);
        product.setLastUpdated(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);

        // 캐시 즉시 업데이트
        String cacheKey = CACHE_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(cacheKey, updatedProduct, CACHE_TTL, TimeUnit.SECONDS);

        return updatedProduct;
    }

    public Product getProduct(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;

        // Refresh-Ahead: 캐시 조회 및 갱신 큐 추가
        Product cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            refreshQueue.add(id);
            return cachedProduct;
        }

        return loadFromDatabaseAndCache(id, cacheKey);
    }

    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    public void refreshCache() {
        refreshQueue.forEach(id -> {
            String cacheKey = CACHE_KEY_PREFIX + id;
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);

            if (ttl != null && ttl <= REFRESH_AHEAD_TIME) {
                loadFromDatabaseAndCache(id, cacheKey);
            }
        });
        refreshQueue.clear();
    }

    private Product loadFromDatabaseAndCache(Long id, String cacheKey) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.SECONDS);
        return product;
    }
}