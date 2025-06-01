package org.example.cachinghybridstrategy.server;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ServerMetric {
    @Id
    private String serverId;
    private double cpuUsage;
    private double memoryUsage;
    private int activeConnections;
    private LocalDateTime lastUpdated;
}