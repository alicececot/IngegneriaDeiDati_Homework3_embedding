package org.example;

import java.io.*;
import java.util.*;

public class MetricsCalculator {
    public static double calculateMRR(List<Double> relevanceList) {
        for (int i = 0; i < relevanceList.size(); i++) {
            if (relevanceList.get(i) > 2.5) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    public static double calculateNDCG(List<Double> relevanceList) {
        double dcg = 0.0;
        for (int i = 0; i < relevanceList.size(); i++) {
            dcg += (Math.pow(2, relevanceList.get(i)) - 1) / (Math.log(i + 2) / Math.log(2));
        }

        List<Double> sortedList = new ArrayList<>(relevanceList);
        sortedList.sort(Collections.reverseOrder());
        double idcg = 0.0;
        for (int i = 0; i < sortedList.size(); i++) {
            idcg += (Math.pow(2, sortedList.get(i)) - 1) / (Math.log(i + 2) / Math.log(2));
        }
        return idcg > 0 ? dcg / idcg : 0.0;
    }

    public static void main(String[] args) {
        String filePath = "relevance_results.csv";
        Map<String, Map<String, List<Double>>> results = new LinkedHashMap<>();
        List<Double> mrrEmbedding = new ArrayList<>();
        List<Double> mrrFullText = new ArrayList<>();
        List<Double> ndcgEmbedding = new ArrayList<>();
        List<Double> ndcgFullText = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String query = values[0].trim();
                String searchType = values[7].trim();
                double relevance = Double.parseDouble(values[6]);

                results.computeIfAbsent(query, k -> new LinkedHashMap<>())
                        .computeIfAbsent(searchType, k -> new ArrayList<>())
                        .add(relevance);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, Map<String, List<Double>>> queryEntry : results.entrySet()) {
            String query = queryEntry.getKey();
            for (Map.Entry<String, List<Double>> searchTypeEntry : queryEntry.getValue().entrySet()) {
                String searchType = searchTypeEntry.getKey();
                List<Double> relevanceList = searchTypeEntry.getValue();
                for (int i = 0; i < relevanceList.size(); i += 5) {
                    List<Double> subList = relevanceList.subList(i, Math.min(i + 5, relevanceList.size()));
                    double mrr = calculateMRR(subList);
                    double ndcg = calculateNDCG(subList);
                    System.out.println("Query: " + query + ", Search Type: " + searchType);
                    System.out.println("MRR: " + mrr);
                    System.out.println("NDCG: " + ndcg);
                    if (searchType.equals("embedding")) {
                        mrrEmbedding.add(mrr);
                        ndcgEmbedding.add(ndcg);
                    } else if (searchType.equals("full-text")) {
                        mrrFullText.add(mrr);
                        ndcgFullText.add(ndcg);
                    }
                }
            }
        }

        double avgMrrEmbedding = mrrEmbedding.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMrrFullText = mrrFullText.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgNdcgEmbedding = ndcgEmbedding.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgNdcgFullText = ndcgFullText.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        System.out.println("Average MRR for Embedding: " + avgMrrEmbedding);
        System.out.println("Average MRR for Full-Text: " + avgMrrFullText);
        System.out.println("Average NDCG for Embedding: " + avgNdcgEmbedding);
        System.out.println("Average NDCG for Full-Text: " + avgNdcgFullText);
    }
}
