package org.example.rankingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.rankingsystem.dto.UserRankDto;
import org.example.rankingsystem.dto.UserScoreAddDto;
import org.example.rankingsystem.global.RsData;
import org.example.rankingsystem.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 점수 추가
    @PostMapping("/score")
    public ResponseEntity<RsData<UserScoreAddDto>> addScore(@RequestParam String userId, @RequestParam double score) {
        RsData<UserScoreAddDto> response = rankingService.addScore(userId, score);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // 유저 랭킹 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<RsData<UserRankDto>> getUserRank(@PathVariable String userId) throws Exception {
        Long rank = rankingService.getUserRank(userId);
        Double score = rankingService.getUserScore(userId);

        if (rank == null || score == null) {
            throw new Exception("rank or score is null");
        }

        RsData<UserRankDto> response = new RsData<>("200", "랭킹 조회가 완료되었습니다.", UserRankDto.from(rank, userId, score));

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // 상위 N명 랭킹 조회
    @GetMapping("/top")
    public ResponseEntity<List<UserRankDto>> getTopRank(@RequestParam(defaultValue = "10") int count) throws Exception {
        return ResponseEntity.ok(rankingService.getTopRank(count));
    }
}
