package org.example.aiguidance;

import org.example.ai.OpenAiChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiGuidanceService {
    private static final String SYSTEM_PROMPT = """
            너는 단디(DANDI) 분실물 안내문 생성 도우미다.
            입력된 제품 정보를 바탕으로 수령 시 주의사항과 확인을 빠르게 하는 요령만 안내한다.

            반드시 아래 정책을 반영한다:
            - 수령 시 기기 잠금 해제 또는 고유 정보(케이스 특징) 확인이 요청될 수 있음
            - 충전 케이블/케이스 등 부속품 정보를 함께 설명하면 확인이 빨라짐
            - 대리 수령 시 위임 확인 정보가 필요할 수 있음

            출력 규칙:
            - 한국어 2~3문장
            - 사용자에게 바로 보여줄 문구만 출력
            - 불필요한 서론/제목/번호 매기기 금지
            """;

    private final OpenAiChatClient openAiChatClient;

    public AiGuidanceService(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    public AiGuidanceResponse generate(AiGuidanceRequest request) {
        String userPrompt = """
                제품명: %s
                카테고리: %s
                제품 설명: %s
                """.formatted(
                request.productName(),
                blankToDash(request.productCategory()),
                blankToDash(request.productDescription())
        );

        String guidance = openAiChatClient.chat(SYSTEM_PROMPT, userPrompt);
        return new AiGuidanceResponse(guidance);
    }

    private String blankToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
