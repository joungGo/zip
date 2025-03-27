package org.example.springbootpubsub.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto implements Serializable {

    @Serial // 직렬화 버전 관리: 직렬화된 객체를 역직렬화 할 때 클래스 버전과 같은지 확인
    private static final long serialVersionUID = 1L; // 직렬화 버전 관리

    private String message; // 전송할 메세지 내용
    private String sender; // 메세지 발신자
    private String roomId; // 메세지 방 번호 || 타겟 Channel
}