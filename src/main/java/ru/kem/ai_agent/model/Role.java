package ru.kem.ai_agent.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER("user") {
        Message getMessage(String prompt) {
            return new UserMessage(prompt);
        }
    },
    ASSISTANT("assistant") {
        Message getMessage(String prompt) {
            return new AssistantMessage(prompt);
        }
    },
    SYSTEM("system") {
        Message getMessage(String prompt) {
            return new SystemMessage(prompt);
        }
    };

    private final String role;

    public static Role getRole(String roleName) {
        return Arrays.stream(Role.values())
                .filter(role -> role.role.equals(roleName))
                .findFirst()
                .orElseThrow();
    }

    abstract Message getMessage(String prompt);
}
