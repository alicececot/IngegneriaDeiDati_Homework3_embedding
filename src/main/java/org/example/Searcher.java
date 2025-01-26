package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Searcher {

    private static final String INDEX_DIR = "index";
    private final IndexSearcher searcher;
    private final Analyzer analyzer;
    private final EmbeddingModel embeddingModel;

    public Searcher() throws IOException {
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIR));
        IndexReader reader = DirectoryReader.open(directory);
        this.searcher = new IndexSearcher(reader);

        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("caption", myAnalyzer());
        perFieldAnalyzers.put("table", myAnalyzer());
        perFieldAnalyzers.put("footnotes", myAnalyzer());
        perFieldAnalyzers.put("references", myAnalyzer());

        this.analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);

        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    }

    private Analyzer myAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(WhitespaceTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
    }

    public List<Map<String, String>> search(String query) throws Exception {
        String[] fieldsQuery = {"caption", "table", "footnotes", "references"};
        Map<String, Float> boosts = new HashMap<>();
        boosts.put("caption", 2.0f);
        boosts.put("table", 2.0f);
        boosts.put("footnotes", 0.5f);
        boosts.put("references", 1.5f);

        MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsQuery, analyzer, boosts);
        Query multiFieldQuery = parser.parse(query);

        TopDocs results = searcher.search(multiFieldQuery, 50);
        return extractResults(results);
    }

    public List<Map<String, String>> searchWithEmbeddings(String queryText) throws IOException {
        TextSegment querySegment = TextSegment.from(queryText);
        Embedding queryEmbedding = embeddingModel.embed(querySegment).content();

        KnnFloatVectorQuery knnQuery = new KnnFloatVectorQuery("embedding", queryEmbedding.vector(), 50);
        TopDocs results = searcher.search(knnQuery, 50);
        return extractResults(results);
    }

    private List<Map<String, String>> extractResults(TopDocs results) throws IOException {
        List<Map<String, String>> resultsList = new ArrayList<>();
        IndexReader reader = searcher.getIndexReader();

        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = reader.storedFields().document(scoreDoc.doc);
            Map<String, String> result = new HashMap<>();
            for (String key : new String[]{"filename", "tableKey", "caption", "table", "footnotes", "references"}) {
                result.putIfAbsent(key, doc.get(key));
            }
            resultsList.add(result);
        }
        return resultsList;
    }



}
