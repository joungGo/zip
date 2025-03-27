package org.example.rankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.rankingsystem.dto.UserRankDto;
import org.example.rankingsystem.dto.UserScoreAddDto;
import org.example.rankingsystem.global.RsData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final String RANKING_KEY = "userRanking";
    private final RedisTemplate<String, String> redisTemplate;

    // 유저 점수 추가 또는 업데이트
    public RsData<UserScoreAddDto> addScore(String userId, double score) {
        redisTemplate.opsForZSet().add(RANKING_KEY, userId, score); // opsForZSet() 은 ZSetOperations<String, String> 객체를 반환 -> ZSetOperations<String, String> 객체는 Redis 의 Sorted Set 데이터 타입을 다루는데 사용
        return new RsData<>("200", "점수가 성공적으로 추가되었습니다.", UserScoreAddDto.from(userId, score));
    }

    // 특정 유저(userId)의 현재 랭킹 조회 (1등부터 시작)
    public Long getUserRank(String userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, userId); // 내림차순 정렬 후 조회
        return (rank == null) ? null : rank + 1; // +1을 하는 이유는 0부터 시작하기 때문에 1을 더해줘야 함
        // 4   3   2   1 (rank)
        // 3   2   1   0 (index)
    }

    // 특정 유저의 점수 조회
    public Double getUserScore(String userId) {
        return redisTemplate.opsForZSet().score(RANKING_KEY, userId);
    }

    // 상위 N명의 랭킹 조회
    public List<UserRankDto> getTopRank(int topN) throws Exception {
        Set<ZSetOperations.TypedTuple<String>> topRanks = redisTemplate.opsForZSet() // ZSetOperations.TypedTuple<String>: ZSetOperations 의 결과를 담는 객체 (value, score)
                // .TypedTuple: Redis 에서 반환된 값과 점수를 포함한 객체
                .reverseRangeWithScores(RANKING_KEY, 0, topN - 1/*index 라서 1빼는 거임*/); // 내림차순 정렬 후 상위 N명 조회
                // 100  25   13   4 (score)
                // 0  1   2   3 (index)

        if (topRanks == null) {
            throw new Exception("topRanks is null");
        }

        List<UserRankDto> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : topRanks) { // ZSetOperations.TypedTuple<String>은 Redis 에서 반환된 값과 점수를 포함한 객체입니다. tuple.getValue()로 유저 ID를, tuple.getScore()로 점수를 가져올 수 있습니다.
            result.add(new UserRankDto(rank++, tuple.getValue(), tuple.getScore()));
        }
        return result;
    }
}
