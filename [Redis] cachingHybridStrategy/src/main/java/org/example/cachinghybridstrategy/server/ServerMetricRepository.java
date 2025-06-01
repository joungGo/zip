package org.example.cachinghybridstrategy.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServerMetricRepository extends JpaRepository<ServerMetric, String> {
    List<ServerMetric> findAllByLastUpdatedAfter(LocalDateTime time);
} 