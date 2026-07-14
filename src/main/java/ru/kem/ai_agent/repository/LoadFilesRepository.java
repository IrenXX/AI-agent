package ru.kem.ai_agent.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.kem.ai_agent.model.LoadFiles;


@Repository
public interface LoadFilesRepository extends CrudRepository<LoadFiles, Long> {

    boolean existsByFileNameAndContentHash(String fileName, String contentHash);
}