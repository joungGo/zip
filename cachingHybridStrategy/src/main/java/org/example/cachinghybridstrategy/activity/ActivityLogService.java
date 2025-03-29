package org.example.cachinghybridstrategy.activity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository logRepository;
    private final RedisTemplate<String, ActivityLog> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "log:activity:";
    private static final long CACHE_TTL = 7200; // 2시간

    private final Map<String, ActivityLog> writeQueue = new ConcurrentHashMap<>();
    private static final int BATCH_SIZE = 100;

    public List<ActivityLog> getAllActivities() {
        return logRepository.findAll();
    }

    public ActivityLog getActivityById(String id) {
        return getLog(id);
    }

    public ActivityLog createActivity(ActivityLog activityLog) {
        logActivity(activityLog);
        return activityLog;
    }

    public ActivityLog updateActivity(String id, ActivityLog activityLog) {
        ActivityLog existingLog = getLog(id);
        activityLog.setId(id);
        logActivity(activityLog);
        return activityLog;
    }

    public void deleteActivity(String id) {
        logRepository.deleteById(id);
        redisTemplate.delete(CACHE_KEY_PREFIX + id);
        writeQueue.remove(id);
    }

    public void logActivity(ActivityLog log) {
        // Write-Behind: 로그를 캐시와 큐에 저장
        String cacheKey = CACHE_KEY_PREFIX + log.getId();
        redisTemplate.opsForValue().set(cacheKey, log, CACHE_TTL, TimeUnit.SECONDS);
        writeQueue.put(log.getId(), log);

        // 큐가 가득 차면 즉시 처리
        if (writeQueue.size() >= BATCH_SIZE) {
            processWriteQueue();
        }
    }

    // Cache-Aside: 로그 조회
    @Cacheable(value = "activityLogs", key = "#id")
    public ActivityLog getLog(String id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
    }

    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    @Transactional
    public void processWriteQueue() {
        if (writeQueue.isEmpty()) return;

        List<ActivityLog> logsToSave = new ArrayList<>(writeQueue.values());
        try {
            logRepository.saveAll(logsToSave);
            logsToSave.forEach(log -> writeQueue.remove(log.getId()));
        } catch (Exception e) {
            log.error("Failed to save logs: ", e);
            // 실패한 로그 처리 로직 구현
        }
    }

    public List<ActivityLog> getUnprocessedLogs() {
        return new ArrayList<>(writeQueue.values());
    }
}