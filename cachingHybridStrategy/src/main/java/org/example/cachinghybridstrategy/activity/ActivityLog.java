package org.example.cachinghybridstrategy.activity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ActivityLog {
    @Id
    private String id;
    private String userId;
    private String action;
    private String details;
    private LocalDateTime timestamp;
    private boolean processed;
}