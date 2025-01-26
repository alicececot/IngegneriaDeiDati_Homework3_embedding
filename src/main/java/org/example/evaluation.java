package org.example;

import org.example.Searcher;
import java.io.*;
import java.util.*;

public class evaluation {

    public static void main(String[] args) throws Exception {
        String[] testQueries = {
                "accuracy on ImageNet dataset for ResNet",
                "performance comparison on CIFAR-10 dataset",
                "hyperparameter tuning meta-learning methods",
                "accuracy on MNIST CNN",
                "Recall of R-CNN on COCO dataset"
        };

        Searcher searcher = new Searcher();
        Scanner scanner = new Scanner(System.in);

        FileWriter writer = new FileWriter("relevance_results.csv");
        writer.append("query,filename,table ID,semantic_relevance,info_completeness,domain_relevance,average_relevance,search_type\n");

        for (String query : testQueries) {
            for (int mode = 1; mode <= 2; mode++) {
                String searchType = (mode == 1) ? "full-text" : "embedding";
                System.out.println("Eseguendo la ricerca per la query: " + query + " con " + searchType);

                List<Map<String, String>> results;
                if (mode == 1) {
                    results = searcher.search(query);
                } else {
                    results = searcher.searchWithEmbeddings(query);
                }

                for (int i = 0; i < Math.min(5, results.size()); i++) {
                    Map<String, String> result = results.get(i);
                    System.out.println("Documento " + (i + 1) + ": " + result.get("filename") + " | Table ID: " + result.get("tableKey"));

                    System.out.print("Valuta la corrispondenza semantica (0-3): ");
                    int semanticRelevance = scanner.nextInt();

                    System.out.print("Valuta il contenuto informativo e completezza (0-3): ");
                    int infoCompleteness = scanner.nextInt();

                    System.out.print("Valuta il contesto e la rilevanza del dominio (0-3): ");
                    int domainRelevance = scanner.nextInt();
                    scanner.nextLine();  // Consuma l'input extra

                    double averageRelevance = (semanticRelevance + infoCompleteness + domainRelevance) / 3.0;

                    writer.append(query + "," + result.get("filename") + "," + result.get("tableKey") + "," + semanticRelevance + "," + infoCompleteness + "," + domainRelevance + "," + averageRelevance + "," + searchType + "\n");
                }
            }
        }
        writer.flush();
        writer.close();

        System.out.println("Valutazione completata. I risultati sono stati salvati in 'relevance_results.csv'.");
    }
}
