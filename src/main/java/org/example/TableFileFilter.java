package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class TableFileFilter {

    public static void main(String[] args) {
        String sourceDirectory = "/Users/alicececot/IdeaProjects/HomeworkEmbedding/src/main/java/org/example/clear_tables";
        String destinationDirectory = "/Users/alicececot/IdeaProjects/HomeworkEmbedding/src/main/java/org/example/final_clear_tables";

        try {
            copyValidJsonFiles(sourceDirectory, destinationDirectory);
            System.out.println("File filtrati e copiati con successo!");
        } catch (IOException e) {
            System.err.println("Errore durante la copia dei file: " + e.getMessage());
        }
    }

    public static void copyValidJsonFiles(String sourceDir, String destDir) throws IOException {
        File sourceFolder = new File(sourceDir);
        File destinationFolder = new File(destDir);

        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
        }

        File[] jsonFiles = sourceFolder.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles != null) {
            ObjectMapper objectMapper = new ObjectMapper();

            for (File jsonFile : jsonFiles) {
                try {
                    JsonNode rootNode = objectMapper.readTree(jsonFile);

                    boolean hasValidTable = false;
                    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        JsonNode tableNode = entry.getValue().get("table");

                        if (tableNode != null && tableNode.isTextual() && !tableNode.asText().trim().isEmpty()) {
                            hasValidTable = true;
                            break;
                        }
                    }

                    if (hasValidTable) {
                        FileUtils.copyFileToDirectory(jsonFile, destinationFolder);
                        System.out.println("File copiato: " + jsonFile.getName());
                    } else {
                        System.out.println("File ignorato (nessuna tabella valida): " + jsonFile.getName());
                    }

                } catch (Exception e) {
                    System.err.println("Errore nella lettura del file: " + jsonFile.getName() + " - " + e.getMessage());
                }
            }
        } else {
            System.err.println("Nessun file JSON trovato nella cartella: " + sourceDir);
        }
    }
}
