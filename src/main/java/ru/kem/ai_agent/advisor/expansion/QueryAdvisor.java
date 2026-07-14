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
import org.springframework.ai.ollama.api.OllamaChatOptions;

@Builder
@RequiredArgsConstructor
public class QueryAdvisor implements BaseAdvisor {

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
        String extenderQuery = chatClient.prompt().user("ба-цзы").call().content();

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
