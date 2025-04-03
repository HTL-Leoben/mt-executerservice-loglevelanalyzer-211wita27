package Analyse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

public class LogAnalyzerTask implements Callable<AnalysisResult> {

    private final Path logFile;

    // Für die Fehlertyp-Analyse (Aufgabenstellung 2) definieren wir ein paar bekannte Stichwörter:
    private static final String[] KNOWN_ERRORS = {
            "NullPointerException",
            "FileNotFoundException",
            "SQLException",
            "OutOfMemoryError",
            "ArrayIndexOutOfBoundsException"
    };

    public LogAnalyzerTask(Path logFile) {
        this.logFile = logFile;
    }

    @Override
    public AnalysisResult call() throws Exception {
        // Zählungen der LogLevel
        Map<String, Integer> levelCount = createEmptyLevelMap();
        // Liste der WARN/ERROR-Zeilen
        List<String> warnErrorLines = new ArrayList<>();
        // Zählung bestimmter Fehlertypen
        Map<String, Integer> errorTypeCount = new HashMap<>();

        // Initialisiere das Fehlertypen-Map
        for (String errorKey : KNOWN_ERRORS) {
            errorTypeCount.put(errorKey, 0);
        }

        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+", 3);
                if (parts.length < 2) {
                    continue; // Zeile ist zu kurz oder ungültig
                }

                String level = parts[1];
                // LogLevel zählen
                if (levelCount.containsKey(level)) {
                    levelCount.put(level, levelCount.get(level) + 1);
                }

                // Wenn WARN oder ERROR, füge Zeile in warnErrorLines ein
                if ("WARN".equals(level) || "ERROR".equals(level)) {
                    warnErrorLines.add(line);

                    // Prüfe zusätzlich auf bekannte Fehlertypen
                    for (String errorKeyword : KNOWN_ERRORS) {
                        if (line.contains(errorKeyword)) {
                            errorTypeCount.put(errorKeyword, errorTypeCount.get(errorKeyword) + 1);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen von " + logFile + ": " + e.getMessage());
        }

        return new AnalysisResult(logFile, levelCount, warnErrorLines, errorTypeCount);
    }

    // Hilfsmethode: Erstellt eine Map, in der alle relevanten LogLevel auf 0 gesetzt sind.
    private Map<String, Integer> createEmptyLevelMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("TRACE", 0);
        map.put("DEBUG", 0);
        map.put("INFO",  0);
        map.put("WARN",  0);
        map.put("ERROR", 0);
        return map;
    }
}