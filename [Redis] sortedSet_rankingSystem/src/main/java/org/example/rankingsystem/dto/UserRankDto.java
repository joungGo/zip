package org.example.rankingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class UserRankDto {
    private int rank;
    private String userId;
    private Double score;

    public static UserRankDto from(Long rank, String userId, Double score) {
        return UserRankDto.builder()
                .rank(rank.intValue())
                .userId(userId)
                .score(score)
                .build();
    }
}
