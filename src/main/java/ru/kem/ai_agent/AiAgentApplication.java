package ru.kem.ai_agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.kem.ai_agent.advisor.expansion.QueryAdvisor;
import ru.kem.ai_agent.advisor.rag.RagAdvisor;
import ru.kem.ai_agent.repository.ChatMemoryPostgres;
import ru.kem.ai_agent.repository.ChatRepository;

@SpringBootApplication
@RequiredArgsConstructor
public class AiAgentApplication {

    private final ChatRepository chatRepository;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    private static final PromptTemplate MY_PROMPT_TEMPLATE = new PromptTemplate
            ("""
                    {query}\n\nКонтекстная информация приведена ниже, в рамке ---------------------\n\n---------------------\n
                    {question_answer_context}\n---------------------
                    \n\nУчитывая контекст и предоставленную историческую информацию, а не предварительные знания,
                    \nответь на комментарий пользователя. Если ответ не соответствует контексту, сообщите об этом
                    \nпользователю, на вопрос которого не можешь ответить.\n
            """);


    static void main(String[] args) {
        /*ChatClient chatClient = */
        SpringApplication.run(AiAgentApplication.class, args).getBean(ChatClient.class);
//        System.out.println(chatClient.prompt().user("Сколько будет 2+2?").call().content());
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultAdvisors(
                        QueryAdvisor.builder(chatModel).order(0).build(),
                        getHistoryAdvisor(1),
                        SimpleLoggerAdvisor.builder().order(2).build(),
                        /*getRagAdvisor(3),*/
                        RagAdvisor.builder(vectorStore).order(3).build(),
                        SimpleLoggerAdvisor.builder().order(4).build())
                .defaultOptions(OllamaChatOptions.builder().temperature(0.3).topP(0.7).topK(20).repeatPenalty(1.1))
                .build();
    }

    private Advisor getRagAdvisor(int order) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(MY_PROMPT_TEMPLATE)
                .searchRequest(SearchRequest.builder().topK(4).similarityThreshold(0.67).build())
                .order(order)
                .build();
    }

    private Advisor getHistoryAdvisor(int order) {
        return MessageChatMemoryAdvisor.builder(getChatMemory()).order(order).build();
    }

    private ChatMemory getChatMemory() {
        return ChatMemoryPostgres.builder()
                .maxMessages(2)
                .chatRepository(chatRepository)
                .build();
    }

}
