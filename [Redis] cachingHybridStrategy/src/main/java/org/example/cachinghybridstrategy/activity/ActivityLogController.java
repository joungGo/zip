package org.example.cachinghybridstrategy.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    @GetMapping
    public ResponseEntity<List<ActivityLog>> getAllActivities() {
        return ResponseEntity.ok(activityLogService.getAllActivities());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ActivityLog> getActivityById(@PathVariable String id) {
        return ResponseEntity.ok(activityLogService.getActivityById(id));
    }
    
    @PostMapping
    public ResponseEntity<ActivityLog> createActivity(@RequestBody ActivityLog activityLog) {
        return ResponseEntity.ok(activityLogService.createActivity(activityLog));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ActivityLog> updateActivity(@PathVariable String id, @RequestBody ActivityLog activityLog) {
        return ResponseEntity.ok(activityLogService.updateActivity(id, activityLog));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String id) {
        activityLogService.deleteActivity(id);
        return ResponseEntity.ok().build();
    }
} 