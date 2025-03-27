package org.example.rankingsystem.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class UserScoreAddDto {
    private String userId;
    private Double score;

    public static UserScoreAddDto from(String userId, Double score) {
        return UserScoreAddDto.builder()
                .userId(userId)
                .score(score)
                .build();
    }
}
