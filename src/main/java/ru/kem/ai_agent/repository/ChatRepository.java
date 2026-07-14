package ru.kem.ai_agent.repository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.kem.ai_agent.model.Chat;

@Repository
public interface ChatRepository extends CrudRepository<Chat, Long> {
    List<Chat> findAll(Sort createdAt);
}
