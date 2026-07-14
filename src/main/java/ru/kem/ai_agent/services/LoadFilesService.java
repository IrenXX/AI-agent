package ru.kem.ai_agent.services;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import ru.kem.ai_agent.model.LoadFiles;
import ru.kem.ai_agent.repository.LoadFilesRepository;

@Service
@RequiredArgsConstructor
public class LoadFilesService implements CommandLineRunner {

    private final LoadFilesRepository repository;
    private final ResourcePatternResolver resolver;
    private final VectorStore vectorStore;

    @SneakyThrows
    public void loadFiles() {
        List<Resource> resources = Arrays.stream(resolver.getResources("classpath:/documents/**/*.txt")).toList();
        resources.stream()
                .map(resource -> Pair.of(resource, calcContentHash(resource)))
                .filter(pair ->
                        !repository.existsByFileNameAndContentHash(pair.getFirst().getFilename(), pair.getSecond()))
                .forEach(pair -> {
                    List<Document> documents = new TextReader(pair.getFirst()).get();
                    TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(200).build();
                    List<Document> chunks = textSplitter.apply(documents);
                    vectorStore.accept(chunks);

                    LoadFiles file = LoadFiles.builder()
                            .documentType("txt")
                            .chunkCount(chunks.size())
                            .fileName(pair.getFirst().getFilename())
                            .contentHash(pair.getSecond())
                            .build();

                    repository.save(file);
                });
    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    @Override
    public void run(String... args) {
        loadFiles();
    }
}
