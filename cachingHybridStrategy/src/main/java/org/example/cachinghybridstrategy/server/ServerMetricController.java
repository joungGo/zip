package org.example.cachinghybridstrategy.server;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server-metrics")
@RequiredArgsConstructor
public class ServerMetricController {
    
    private final ServerMonitoringService serverMonitoringService;
    
    @GetMapping
    public ResponseEntity<List<ServerMetric>> getAllServerMetrics() {
        return ResponseEntity.ok(serverMonitoringService.getAllServerMetrics());
    }
    
    @GetMapping("/{serverId}")
    public ResponseEntity<ServerMetric> getServerMetricById(@PathVariable String serverId) {
        return ResponseEntity.ok(serverMonitoringService.getServerMetricById(serverId));
    }
    
    @PostMapping
    public ResponseEntity<ServerMetric> createServerMetric(@RequestBody ServerMetric serverMetric) {
        return ResponseEntity.ok(serverMonitoringService.createServerMetric(serverMetric));
    }
    
    @PutMapping("/{serverId}")
    public ResponseEntity<ServerMetric> updateServerMetric(@PathVariable String serverId, @RequestBody ServerMetric serverMetric) {
        return ResponseEntity.ok(serverMonitoringService.updateServerMetric(serverId, serverMetric));
    }
    
    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServerMetric(@PathVariable String serverId) {
        serverMonitoringService.deleteServerMetric(serverId);
        return ResponseEntity.ok().build();
    }
} 