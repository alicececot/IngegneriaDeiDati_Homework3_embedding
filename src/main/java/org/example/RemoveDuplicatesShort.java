package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveDuplicatesShort {

    private static final String SOURCE_DIR = "/Users/alicececot/Desktop/ingegneria dei dati/urls_htmls_tables/all_tables";
    private static final String CLEANED_DIR = "/Users/alicececot/IdeaProjects/HomeworkEmbedding/src/main/java/org/example/clear_tables";

    private static final Pattern FILE_PATTERN = Pattern.compile("(\\d{4})\\.(\\d+)");

    public static void main(String[] args) {
        File sourceFolder = new File(SOURCE_DIR);
        File cleanedFolder = new File(CLEANED_DIR);

        if (!cleanedFolder.exists()) cleanedFolder.mkdir();

        HashSet<String> uniqueIdentifiers = new HashSet<>();
        File[] jsonFiles = sourceFolder.listFiles((dir, name) ->
                name.endsWith(".json") && !name.startsWith("._")
        );

        if (jsonFiles != null) {
            for (File file : jsonFiles) {
                String numericIdentifier = extractNumericPart(file.getName());

                if (numericIdentifier != null) {
                    if (uniqueIdentifiers.add(numericIdentifier)) {
                        try {
                            Files.copy(file.toPath(), new File(cleanedFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Copied: " + file.getName());
                        } catch (IOException e) {
                            System.err.println("Error copying file: " + file.getName() + " - " + e.getMessage());
                        }
                    } else {
                        System.out.println("Duplicate skipped: " + file.getName());
                    }
                }
            }
        }
        System.out.println("Deduplication completed. Cleaned files are in: " + CLEANED_DIR);
    }

    private static String extractNumericPart(String fileName) {
        Matcher matcher = FILE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String part1 = matcher.group(1);
            String part2 = matcher.group(2);
            return part1 + part2;
        }
        return null;
    }
}