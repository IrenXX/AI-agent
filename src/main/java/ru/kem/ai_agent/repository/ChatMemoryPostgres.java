package ru.kem.ai_agent.repository;


import java.util.List;
import lombok.Builder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import ru.kem.ai_agent.model.Chat;
import ru.kem.ai_agent.model.ChatEntry;

@Builder
public class ChatMemoryPostgres implements ChatMemory {

    private ChatRepository chatRepository;
    private int maxMessages;

    @Override
    public void add(String s, List<Message> list) {
        Chat chat = chatRepository.findById(Long.valueOf(s)).orElseThrow();
        for (Message message : list) {
            chat.addChatEntry(ChatEntry.toChatEntry(message));
        }
        chatRepository.save(chat);
    }

    @Override
    public List<Message> get(String s) {
        Chat chat = chatRepository.findById(Long.valueOf(s)).orElseThrow();
        long skippedMessage = Math.max(0, chat.getHistory().size() - maxMessages);

        return chat.getHistory().stream()
                .skip(skippedMessage)
                .map(ChatEntry::toMessage)
                .toList();
    }

    @Override
    public void clear(String s) {
    }
}
