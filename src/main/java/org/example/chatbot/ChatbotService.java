package org.example.chatbot;

import org.example.ai.OpenAiChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {
    private static final String SYSTEM_PROMPT = """
            너는 단디(DANDI) 분실물 관리자 Q&A 챗봇이다.
            분실물 수령/보관 위치/필요 서류/대리 수령 절차 관련 질문에만 답변한다.

            규칙:
            1) 범위를 벗어난 질문에는 아래 문구 그대로 답한다.
            "저는 단디 분실물 Q&A 챗봇입니다. 분실물 수령/위치/서류 관련 질문만 도와드릴 수 있어요."
            2) 확실하지 않은 운영 정보는 추측하지 말고 담당 부서 확인이 필요하다고 안내한다.
            3) 주민등록번호/계좌번호/비밀번호 같은 민감정보를 요구하지 않는다.
            4) 답변은 한국어로 2~4문장, 간결하게 작성한다.
            """;

    private final OpenAiChatClient openAiChatClient;

    public ChatbotService(OpenAiChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    public ChatbotResponse chat(ChatbotRequest request) {
        String answer = openAiChatClient.chat(SYSTEM_PROMPT, request.message());
        return new ChatbotResponse(answer);
    }
}
