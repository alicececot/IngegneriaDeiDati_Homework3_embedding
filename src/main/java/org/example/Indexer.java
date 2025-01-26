package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Indexer {
    private static final String JSON_DIR = "/Users/alicececot/IdeaProjects/HomeworkEmbedding/src/main/java/org/example/final_clear_tables";
    private static final String INDEX_DIR = "/Users/alicececot/IdeaProjects/HomeworkEmbedding/index";

    public static void main(String[] args) {
        try {
            indexJsonFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void indexJsonFiles() throws IOException {
        long startTime = System.currentTimeMillis();
        int documentCount = 0;
        FSDirectory directory = FSDirectory.open(Paths.get(INDEX_DIR));

        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("caption", myAnalyzer());
        perFieldAnalyzers.put("table", myAnalyzer());
        perFieldAnalyzers.put("footnotes", myAnalyzer());
        perFieldAnalyzers.put("references", myAnalyzer());

        Analyzer analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);


        IndexWriter writer = new IndexWriter(directory, config);

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        ObjectMapper objectMapper = new ObjectMapper();

        File folder = new File(JSON_DIR);
        if (folder.isDirectory()) {
            for (File file : folder.listFiles((dir1, name) -> name.endsWith(".json"))) {
                System.out.println("Processing file: " + file.getName());
                documentCount++;


                JsonNode rootNode = objectMapper.readTree(file);

                for (Iterator<String> it = rootNode.fieldNames(); it.hasNext(); ) {
                    String key = it.next();

                    JsonNode node = rootNode.get(key);
                    if (!node.has("caption") && !node.has("table") && !node.has("references") && !node.has("footnotes")) {
                        System.out.println("Skipping irrelevant key (missing fields): " + key);
                        continue;
                    }

                    String caption = safeExtract(node, "caption");
                    String table = safeExtract(node, "table");
                    String references = safeExtract(node, "references");
                    String footnotes = safeExtract(node, "footnotes");

                    if (caption.trim().isEmpty() && table.trim().isEmpty() && references.trim().isEmpty() && footnotes.trim().isEmpty()) {
                        System.out.println("Skipping irrelevant key (empty or blank content): " + key);
                        continue;
                    }

                    String fullText = caption.trim() + " " + table.trim() + " " + references.trim();

                    if (table.trim().isEmpty()) {
                        System.out.println("Skipping key (empty table): " + key);
                        continue;
                    }

                    Embedding embedding = embeddingModel.embed(TextSegment.from(fullText)).content();


                    Document doc = new Document();
                    doc.add(new StringField("filename", file.getName(), Field.Store.YES));
                    doc.add(new StringField("tableKey", key, Field.Store.YES));
                    doc.add(new TextField("caption", caption, Field.Store.YES));
                    doc.add(new TextField("table", table, Field.Store.YES));
                    doc.add(new TextField("footnotes", footnotes, Field.Store.YES));
                    doc.add(new TextField("references", references, Field.Store.YES));
                    doc.add(new TextField("fullText", fullText, Field.Store.YES));

                    doc.add(new KnnFloatVectorField("embedding", embedding.vector()));

                    writer.addDocument(doc);
                    System.out.println("Indexed: " + key);
                }
            }
        }

        writer.commit();
        writer.close();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double durationInSeconds = duration / 1000.0;

        System.out.println("Indexing completed.");
        System.out.println("Total documents indexed: " + documentCount);
        System.out.println("Total indexing time: " + durationInSeconds + " seconds");
        double averageTimePerDocument = documentCount > 0 ? (durationInSeconds / documentCount) : 0;
        System.out.println("Average time per document: " + averageTimePerDocument + " seconds");
    }

    private static String safeExtract(JsonNode node, String fieldName) {
        if (node.has(fieldName) && node.get(fieldName) != null && !node.get(fieldName).isNull()) {
            JsonNode field = node.get(fieldName);

            if (field.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode element : field) {
                    if (element.isArray()) {
                        for (JsonNode subElement : element) {
                            sb.append(subElement.asText().trim()).append(" ");
                        }
                    } else {
                        sb.append(element.asText().trim()).append(" ");
                    }
                }
                return sb.toString().trim();
            } else {
                return field.asText("").trim();
            }
        }
        return "";
    }


    private static Analyzer myAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(WhitespaceTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
    }
}
