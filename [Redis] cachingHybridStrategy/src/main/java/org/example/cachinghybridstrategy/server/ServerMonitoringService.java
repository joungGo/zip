package org.example.cachinghybridstrategy.server;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerMonitoringService {
    private final ServerMetricRepository metricRepository;
    private final RedisTemplate<String, ServerMetric> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "metric:server:";
    private static final long CACHE_TTL = 60; // 1분
    private static final long REFRESH_AHEAD_TIME = 10; // 10초

    private final Set<String> refreshQueue = ConcurrentHashMap.newKeySet();

    public List<ServerMetric> getAllServerMetrics() {
        return metricRepository.findAll();
    }

    public ServerMetric getServerMetricById(String serverId) {
        return getServerMetrics(serverId);
    }

    public ServerMetric createServerMetric(ServerMetric serverMetric) {
        return updateMetrics(serverMetric.getServerId(), serverMetric);
    }

    public ServerMetric updateServerMetric(String serverId, ServerMetric serverMetric) {
        serverMetric.setServerId(serverId);
        return updateMetrics(serverId, serverMetric);
    }

    public void deleteServerMetric(String serverId) {
        metricRepository.deleteById(serverId);
        redisTemplate.delete(CACHE_KEY_PREFIX + serverId);
        refreshQueue.remove(serverId);
    }

    @Transactional
    public ServerMetric updateMetrics(String serverId, ServerMetric metric) {
        // Write-Through: 메트릭 즉시 저장
        metric.setLastUpdated(LocalDateTime.now());
        ServerMetric savedMetric = metricRepository.save(metric);

        // 캐시 즉시 업데이트
        String cacheKey = CACHE_KEY_PREFIX + serverId;
        redisTemplate.opsForValue().set(cacheKey, savedMetric, CACHE_TTL, TimeUnit.SECONDS);

        return savedMetric;
    }

    public ServerMetric getServerMetrics(String serverId) {
        String cacheKey = CACHE_KEY_PREFIX + serverId;

        // Refresh-Ahead: 캐시 조회 및 갱신 큐 추가
        ServerMetric cachedMetric = redisTemplate.opsForValue().get(cacheKey);
        if (cachedMetric != null) {
            refreshQueue.add(serverId);
            return cachedMetric;
        }

        return loadFromDatabaseAndCache(serverId, cacheKey);
    }

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void refreshCache() {
        refreshQueue.forEach(serverId -> {
            String cacheKey = CACHE_KEY_PREFIX + serverId;
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);

            if (ttl != null && ttl <= REFRESH_AHEAD_TIME) {
                loadFromDatabaseAndCache(serverId, cacheKey);
            }
        });
        refreshQueue.clear();
    }

    private ServerMetric loadFromDatabaseAndCache(String serverId, String cacheKey) {
        ServerMetric metric = metricRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server metrics not found"));
        redisTemplate.opsForValue().set(cacheKey, metric, CACHE_TTL, TimeUnit.SECONDS);
        return metric;
    }

    public Map<String, ServerMetric> getAllActiveServers() {
        List<ServerMetric> metrics = metricRepository.findAllByLastUpdatedAfter(
                LocalDateTime.now().minusMinutes(5));
        return metrics.stream()
                .collect(Collectors.toMap(ServerMetric::getServerId, metric -> metric));
    }
}