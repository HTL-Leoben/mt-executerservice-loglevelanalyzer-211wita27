package Analyse;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ParallelLogLevelAnalyzer {

    public static void main(String[] args) {

        // ---------------------------------------------------
        // 1) Wir sammeln alle *.log-Dateien im aktuellen Ordner:
        // ---------------------------------------------------
        List<Path> logFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "*.log")) {
            for (Path p : stream) {
                logFiles.add(p);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Sammeln der Logdateien: " + e.getMessage());
            return;
        }

        if (logFiles.isEmpty()) {
            System.out.println("Keine .log-Dateien im aktuellen Verzeichnis gefunden!");
            return;
        }

        // ---------------------------------------------------
        // 2) ExecutorService mit fester Threadanzahl erstellen
        // ---------------------------------------------------
        // z.B. Anzahl Threads = Anzahl verfügbarer Prozessoren
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        // Liste für die Futures (Ergebnisse)
        List<Future<AnalysisResult>> futures = new ArrayList<>();

        // ---------------------------------------------------
        // 3) Für jede Logdatei eine Callable-Instanz erstellen
        //    und submitten
        // ---------------------------------------------------
        for (Path file : logFiles) {
            LogAnalyzerTask task = new LogAnalyzerTask(file);
            Future<AnalysisResult> future = executorService.submit(task);
            futures.add(future);
        }

        // ---------------------------------------------------
        // 4) Alle Ergebnisse einsammeln und aggregieren
        // ---------------------------------------------------
        // Erstelle Maps für die Gesamtzählung
        Map<String, Integer> globalLevelCount = createEmptyLevelMap();
        Map<String, Integer> globalErrorTypeCount = new HashMap<>();

        // Für die erkannten Fehlertypen sammeln wir alle Keys erst zur Sicherheit
        // (falls du verschiedene Task-Instanzen mit unterschiedlichen KNOWN_ERRORS hast).
        // Hier gehen wir davon aus, dass alle Instanzen dieselbe Config haben.
        // Du kannst aber auch dynamisch auffüllen, falls du mehrere Stichwörter scannen willst.
        globalErrorTypeCount.put("NullPointerException", 0);
        globalErrorTypeCount.put("FileNotFoundException", 0);
        globalErrorTypeCount.put("SQLException", 0);
        globalErrorTypeCount.put("OutOfMemoryError", 0);
        globalErrorTypeCount.put("ArrayIndexOutOfBoundsException", 0);

        long startTime = System.currentTimeMillis();

        // Hole die Ergebnisse ab
        for (Future<AnalysisResult> fut : futures) {
            try {
                AnalysisResult result = fut.get(); // blockiert, bis Task fertig
                // Pro Datei ausgeben
                printSingleFileResult(result);

                // In die globale Zählung integrieren
                for (Map.Entry<String, Integer> entry : result.getLevelCounts().entrySet()) {
                    String level = entry.getKey();
                    int count = entry.getValue();
                    globalLevelCount.put(level, globalLevelCount.get(level) + count);
                }

                // Fehlertypen hochzählen
                for (Map.Entry<String, Integer> eType : result.getErrorTypeCounts().entrySet()) {
                    String errorKey = eType.getKey();
                    int count = eType.getValue();
                    globalErrorTypeCount.put(errorKey, globalErrorTypeCount.get(errorKey) + count);
                }

            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Fehler beim Einsammeln der Future-Ergebnisse: " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ---------------------------------------------------
        // 5) Gesamtergebnis ausgeben
        // ---------------------------------------------------
        System.out.println("\n\n--- PARALLELE GESAMTAUSWERTUNG ---");
        System.out.println("Gesamtzählung aller LogLevel:");
        printLevelMap(globalLevelCount);

        System.out.println("\nFehlertypen-Gesamtübersicht:");
        for (String err : globalErrorTypeCount.keySet()) {
            System.out.printf("%-30s: %d%n", err, globalErrorTypeCount.get(err));
        }

        System.out.println("\nGesamtzeit (ms): " + duration);
        System.out.println("------------------------------------");

        // ExecutorService herunterfahren
        executorService.shutdown();
    }

    // Zeigt Details eines einzelnen Dateien-Ergebnisses
    private static void printSingleFileResult(AnalysisResult result) {
        System.out.println("\nErgebnis für Datei: " + result.getLogFile().getFileName());
        printLevelMap(result.getLevelCounts());

        // (Optional:) Warn-/Error-Zeilen ausgeben (hier nur z.B. die ersten 5)
        List<String> warnErrorLines = result.getWarnErrorLines();
        System.out.println("WARN/ERROR-Zeilen (max. 5):");
        for (int i = 0; i < warnErrorLines.size() && i < 5; i++) {
            System.out.println("  " + warnErrorLines.get(i));
        }

        // Fehlertypen in dieser Datei
        System.out.println("Fehlertypen in dieser Datei:");
        for (Map.Entry<String, Integer> e : result.getErrorTypeCounts().entrySet()) {
            if (e.getValue() > 0) {
                System.out.println("  " + e.getKey() + ": " + e.getValue());
            }
        }
    }

    // Map für globale LogLevel-Zählungen initialisieren
    private static Map<String, Integer> createEmptyLevelMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("TRACE", 0);
        map.put("DEBUG", 0);
        map.put("INFO",  0);
        map.put("WARN",  0);
        map.put("ERROR", 0);
        return map;
    }

    // Konsolen-Ausgabe einer LogLevel-Map
    private static void printLevelMap(Map<String, Integer> levelMap) {
        for (String level : levelMap.keySet()) {
            System.out.printf("  %-5s : %d%n", level, levelMap.get(level));
        }
    }
}