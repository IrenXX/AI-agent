package ru.kem.ai_agent.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.kem.ai_agent.model.Chat;
import static ru.kem.ai_agent.model.Role.ASSISTANT;
import static ru.kem.ai_agent.model.Role.USER;
import ru.kem.ai_agent.repository.ChatRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepo;
    private final ChatClient chatClient;
    private final ChatEntryService chatEntryService;

    @SneakyThrows
    private static void processToken(ChatResponse response, SseEmitter emitter, StringBuilder answer) {
        var token = response.getResult() != null ? response.getResult().getOutput() : null;
        emitter.send(token);
        answer.append(token.getText());
    }

    public List<Chat> getAllChats() {
        return chatRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Chat createNewChat(String title) {
        Chat chat = Chat.builder().title(title).build();
        chatRepo.save(chat);
        return chat;
    }

    public Chat getChat(Long chatId) {
        return chatRepo.findById(chatId).orElseThrow();
    }

    public void deleteChat(Long chatId) {
        chatRepo.deleteById(chatId);
    }

    public void execute(Long chatId, String prompt) {
        chatEntryService.addChatEntry(chatId, prompt, USER);
        String answer = chatClient.prompt().user(prompt).call().content();
        chatEntryService.addChatEntry(chatId, answer, ASSISTANT);
    }

    public SseEmitter executeWithStreaming(Long chatId, String userPrompt) {
//        chatEntryService.addChatEntry(chatId, userPrompt, USER);

        SseEmitter sseEmitter = new SseEmitter(0L);
        final StringBuilder answer = new StringBuilder();

        chatClient
                .prompt(userPrompt)
                .advisors(spec -> spec
                        .param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response) -> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete
                );
        return sseEmitter;
    }
}
