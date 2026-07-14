package ru.kem.ai_agent.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kem.ai_agent.model.Chat;
import ru.kem.ai_agent.model.ChatEntry;
import ru.kem.ai_agent.model.Role;
import ru.kem.ai_agent.repository.ChatRepository;

@Service
@RequiredArgsConstructor
public class ChatEntryService {

    private final ChatRepository chatRepo;

    @Transactional
    public void addChatEntry(Long chatId, String content, Role role) {
        Chat chat = chatRepo.findById(chatId).orElseThrow();
        chat.addChatEntry(ChatEntry.builder().content(content).role(role).build());
    }
}
