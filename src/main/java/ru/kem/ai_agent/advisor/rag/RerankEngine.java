package ru.kem.ai_agent.advisor.rag;


import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.ai.document.Document;

@Builder
public class RerankEngine {
    @Builder.Default
    private static final LanguageDetector languageDetector = LanguageDetectorBuilder
            .fromLanguages(Language.ENGLISH, Language.RUSSIAN)
            .build();

    // BM25 parameters
    @Builder.Default
    private final double K = 1.2;
    @Builder.Default
    private final double B = 0.75;

    public List<Document> rerank(List<Document> documents, String query, int limit) {

        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        DocumentsStatistics stats = documentsStats(documents);

        List<String> queryTerms = tokenizeQuery(query);

        return documents.stream()
                .sorted((d1, d2) ->
                        Double.compare(
                            score(queryTerms, d2, stats),
                            score(queryTerms, d1, stats)
                ))
                .limit(limit)
                .toList();
    }

    private DocumentsStatistics documentsStats(List<Document> docs) {
        Map<String, Integer> docFreq = new HashMap<>();
        Map<Document, List<String>> tokenizedDocs = new HashMap<>();
        int totalLength = 0;
        int totalDocs = docs.size();

        for (Document doc : docs) {
            List<String> tokens = tokenizeQuery(doc.getText());
            tokenizedDocs.put(doc, tokens);
            totalLength += tokens.size();

            Set<String> uniqueTerms = new HashSet<>(tokens);
            uniqueTerms.forEach(term ->
                    docFreq.put(term, docFreq.getOrDefault(term, 0) + 1));
        }

        double avgDocLength = (double) totalLength / totalDocs;

        return new DocumentsStatistics(docFreq, tokenizedDocs, avgDocLength, totalDocs);
    }

    private double score(List<String> queryTerms, Document doc, DocumentsStatistics stats) {
        List<String> tokens = stats.tokenizedDocs.get(doc);
        if (tokens == null) {
            return 0.0;
        }

        // Calculate term frequencies for this document
        Map<String, Integer> termfreqMap = new HashMap<>();
        tokens.forEach(token ->
                termfreqMap.put(token, termfreqMap.getOrDefault(token, 0) + 1));

        int docLength = tokens.size();
        double score = 0.0;

        // Calculate BM25 score
        for (String term : queryTerms) {
            int termfreq = termfreqMap.getOrDefault(term, 0);
            int docfreq = stats.docFreq.getOrDefault(term, 1);

            // BM25 IDF calculation редкость слова - оно поднимает
            double idf = Math.log(1 + (stats.totalDocs - docfreq + 0.5) / (docfreq + 0.5));

            // BM25 term score calculation
            double numerator = termfreq * (K + 1);
            double denominator = termfreq + K * (1 - B + B * docLength / stats.avgDocLength);
            score += idf * (numerator / denominator);
        }

        return score;
    }

    private List<String> tokenizeQuery(String query) {
        List<String> tokens = new ArrayList<>();
        Analyzer analyzer = detectLanguageAnalyzer(query);

        try (TokenStream stream = analyzer.tokenStream(null, query)) {
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
            stream.end();
        } catch (IOException e) {
            throw new RuntimeException("Tokenization error", e);
        }

        return tokens;
    }

    private Analyzer detectLanguageAnalyzer(String query) {
        Language lang = languageDetector.detectLanguageOf(query);
        if (lang == Language.RUSSIAN) {
            return new RussianAnalyzer();
        } else {
            return new EnglishAnalyzer();
        }
    }

    private record DocumentsStatistics(
            Map<String, Integer> docFreq,
            Map<Document, List<String>> tokenizedDocs,
            double avgDocLength,
            int totalDocs) {
    }
}