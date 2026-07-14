package ru.kem.ai_agent.advisor.expansion;


import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.util.Map;

@Builder
@RequiredArgsConstructor
public class QueryAdvisor implements BaseAdvisor {

    private static final PromptTemplate template = PromptTemplate.builder()
            .template("""
                    Ты — ассистент, расширяющий пользовательский запрос перед поиском в базе знаний по китайской системе Ба-цзы (Четыре Столпа Судьбы).
                    Перепиши запрос ниже, дополнив его релевантными терминами предметной области (небесные стволы, земные ветви, пять стихий, десять богов, столпы года/месяца/дня/часа и т.п.), сохранив исходный смысл вопроса.
                    Не отвечай на сам вопрос — верни только расширенную формулировку запроса.
                    
                    ПРАВИЛА:
                    1. Сохрани все слова исходного запроса
                    2. Добавь МАКСИМУМ ПЯТЬ наиболее важных терминов
                    3. Выбирай самые специфичные и релевантные слова
                    4. Результат - простой список слов через пробел

                    Исходный запрос: {query}

                    Расширенный запрос:
                    """)
            .build();
    public static final String ORIGINAL_QUERY = "ORIGINAL_QUERY";
    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String RATIO = "RATIO";
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    @Getter
    private final int order;

    public static QueryAdvisorBuilder builder(ChatModel chatModel) {
        return new QueryAdvisorBuilder().chatClient(ChatClient.builder(chatModel)
                        .defaultOptions(OllamaChatOptions.builder().temperature(0.0).topK(1).topP(0.1))
                .build());
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userQuery = chatClientRequest.prompt().getUserMessage().getText();
        String extenderQuery = chatClient.prompt()
                .user(template.render(Map.of("query", userQuery)))
                .call()
                .content();

        double ratio = extenderQuery.length() /  (double) userQuery.length();


        return chatClientRequest.mutate()
                .context(ORIGINAL_QUERY, userQuery)
                .context(ENRICHED_QUESTION, extenderQuery)
                .context(RATIO, ratio)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}
