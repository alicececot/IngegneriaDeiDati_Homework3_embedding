package org.example;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class Controller {

    private final Searcher searcher;

    public Controller() throws IOException {
        this.searcher = new Searcher();
    }

    @GetMapping("/search")
    public List<Map<String, String>> search(
            @RequestParam String query // Query testuale
    ) {
        List<Map<String, String>> resultsList = new ArrayList<>();
        try {
            // Esegui la ricerca testuale
            List<Map<String, String>> docs = searcher.search(query);
            for (Map<String, String> doc : docs) {
                Map<String, String> result = new HashMap<>();
                result.put("filename", doc.get("filename"));
                result.put("tableKey", doc.get("tableKey"));
                result.put("caption", doc.get("caption"));
                result.put("table", doc.get("table"));
                result.put("footnotes", doc.get("footnotes"));
                result.put("references", doc.get("references"));
                resultsList.add(result);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resultsList;
    }

    @GetMapping("/searchWithEmbeddings")
    public List<Map<String, String>> searchWithEmbeddings(
            @RequestParam String queryText // Query basata su embedding
    ) {
        List<Map<String, String>> resultsList = new ArrayList<>();
        try {
            // Esegui la ricerca con embedding
            List<Map<String, String>> docs = searcher.searchWithEmbeddings(queryText);
            for (Map<String, String> doc : docs) {
                Map<String, String> result = new HashMap<>();
                result.put("filename", doc.get("filename"));
                result.put("tableKey", doc.get("tableKey"));
                result.put("caption", doc.get("caption"));
                result.put("table", doc.get("table"));
                result.put("footnotes", doc.get("footnotes"));
                result.put("references", doc.get("references"));
                resultsList.add(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultsList;
    }
}
