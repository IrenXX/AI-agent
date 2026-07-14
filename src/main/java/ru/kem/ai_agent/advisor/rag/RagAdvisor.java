package ru.kem.ai_agent.advisor.rag;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import static ru.kem.ai_agent.advisor.expansion.QueryAdvisor.ENRICHED_QUESTION;

@Builder
@RequiredArgsConstructor
public class RagAdvisor implements BaseAdvisor {

    @Builder.Default
    private static final PromptTemplate template = PromptTemplate.builder().template(
                    """
                            Context: {context}
                            Question: {question}
                    """)
            .build();

    private final VectorStore vectorStore;

    @Getter
    private final int order;

    public static RagAdvisorBuilder builder(VectorStore vectorStore) {
        return new RagAdvisorBuilder().vectorStore(vectorStore);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String queryRag = Objects.requireNonNull(chatClientRequest.context().getOrDefault(ENRICHED_QUESTION, userQuestion))
                .toString();

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder().query(queryRag).topK(4).similarityThreshold(0.67).build());
        if (documents == null || documents.isEmpty()) {
            return chatClientRequest;
        }

        String llmContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        String finalUserPrompt = template.render(
                Map.of("context", llmContext, "question", userQuestion));

        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(finalUserPrompt)).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}
