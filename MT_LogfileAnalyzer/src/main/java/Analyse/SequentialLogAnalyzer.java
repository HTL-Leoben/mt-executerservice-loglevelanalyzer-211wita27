package Analyse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class SequentialLogAnalyzer {

    // Einfache Methode, um eine einzelne Logdatei sequentiell auszuwerten.
    // Sie gibt eine Map zurück, in der die jeweiligen LogLevel (TRACE, DEBUG, INFO, WARN, ERROR) gezählt werden.
    public Map<String, Integer> analyzeSingleFile(Path logFile) {
        Map<String, Integer> levelCount = createEmptyLevelMap();

        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Einfache Heuristik: Die zweite Spalte enthält das LogLevel, z.B. "TRACE" oder "ERROR".
                // Beispielzeile: 2025-04-03T12:03:15.123 INFO  [main] ...
                // Man kann das natürlich noch robuster parsen, z.B. per Regex.
                String[] parts = line.split("\\s+", 3); // maximal in 3 Teile splitten
                if (parts.length >= 2) {
                    String level = parts[1];
                    if (levelCount.containsKey(level)) {
                        levelCount.put(level, levelCount.get(level) + 1);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei " + logFile + ": " + e.getMessage());
        }

        return levelCount;
    }

    // Führt die sequentielle Analyse für alle übergebenen Logdateien durch
    // und gibt eine Gesamtzusammenfassung zurück.
    public Map<String, Integer> analyzeAllSequentially(Path... logFiles) {
        // Map für die Gesamtzählung aller Dateien
        Map<String, Integer> globalCount = createEmptyLevelMap();

        long startTime = System.currentTimeMillis();

        // Jede Datei wird nacheinander ausgewertet
        for (Path file : logFiles) {
            Map<String, Integer> fileCount = analyzeSingleFile(file);

            // Ergebnis pro Datei ausgeben
            System.out.println("\nErgebnis für Datei: " + file.getFileName());
            printLevelMap(fileCount);

            // In die globale Zählung integrieren
            for (String level : fileCount.keySet()) {
                globalCount.put(level, globalCount.get(level) + fileCount.get(level));
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Gesamtergebnis ausgeben
        System.out.println("\n--- SEQUENTIELLE GESAMTAUSWERTUNG ---");
        printLevelMap(globalCount);
        System.out.println("Gesamtzeit (ms): " + duration);
        System.out.println("-------------------------------------");

        return globalCount;
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

    // Hilfsmethode: Gibt eine Map (LogLevel -> Count) formatiert aus.
    private void printLevelMap(Map<String, Integer> levelMap) {
        for (String level : levelMap.keySet()) {
            System.out.printf("%-5s : %d%n", level, levelMap.get(level));
        }
    }

    // Ein simples main() zum Testen:
    public static void main(String[] args) {
        // Beispiel: wir sammeln alle *.log-Dateien aus dem aktuellen Verzeichnis
        try {
            Path currentDir = Paths.get(".");
            DirectoryStream<Path> logFiles = Files.newDirectoryStream(currentDir, "*.log");

            SequentialLogAnalyzer analyzer = new SequentialLogAnalyzer();
            // Konvertieren von DirectoryStream in Array von Path
            analyzer.analyzeAllSequentially(
                    logFiles
                            .iterator()
                            .next() // NICHT IDEAL: Hier eigentlich in eine Liste einsammeln
            );

            // Achtung: Der obere Code nimmt nur das ERSTE File aus dem Iterator!
            // In der Praxis solltest du alle Logdateien sammeln und übergeben.
            // Siehe unten, wie es richtig aussehen könnte:

            /*
            List<Path> allLogs = new ArrayList<>();
            for (Path p : logFiles) {
                allLogs.add(p);
            }
            analyzer.analyzeAllSequentially(allLogs.toArray(new Path[0]));
            */

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}