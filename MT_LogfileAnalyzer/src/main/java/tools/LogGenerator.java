package tools;

// Test

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LogGenerator {

    private static final Random random = new Random();
    // Formatter für Zeitstempel IN den Log-Einträgen
    private static final DateTimeFormatter LOG_ENTRY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    // Formatter für das Datum IM DATEINAMEN
    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    private static final String[] LOG_LEVELS = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
    private static final String[] THREAD_NAMES = {"main", "worker-1", "worker-2", "http-nio-8080-exec-1", "db-pool-1", "scheduler-task-1"};

    // --- Konfigurierbare Parameter ---
    private int numFiles;         // Anzahl der zu generierenden Dateien (Tage)
    private String filePrefix;    // Präfix für Dateinamen
    private int minSizeKB;        // Minimale Dateigröße in Kilobytes
    private int maxSizeKB;        // Maximale Dateigröße in Kilobytes
    private LocalDate startDate;  // Startdatum für die Log-Dateien
    // ---------------------------------

    public LogGenerator(int numFiles, String filePrefix, LocalDate startDate, int minSizeKB, int maxSizeKB) {
        if (numFiles <= 0) throw new IllegalArgumentException("Anzahl der Dateien muss positiv sein.");
        if (startDate == null) throw new IllegalArgumentException("Startdatum darf nicht null sein.");
        if (minSizeKB < 0) throw new IllegalArgumentException("Minimale Größe darf nicht negativ sein.");
        if (maxSizeKB < minSizeKB) {
            System.out.println("Warnung: Maximale Größe (" + maxSizeKB + "KB) ist kleiner als minimale Größe (" + minSizeKB + "KB). Setze Maximum auf Minimum.");
            this.maxSizeKB = minSizeKB;
        } else {
            this.maxSizeKB = maxSizeKB;
        }

        this.numFiles = numFiles;
        this.filePrefix = (filePrefix != null && !filePrefix.trim().isEmpty()) ? filePrefix.trim() : "app";
        this.startDate = startDate;
        this.minSizeKB = minSizeKB;
        // maxSizeKB wurde oben schon gesetzt
    }

    public void generateLogs() {
        long minSizeBytes = (long) this.minSizeKB * 1024;
        long maxSizeBytes = (long) this.maxSizeKB * 1024;

        System.out.printf("Generiere %d Logdatei(en) ab dem %s mit Präfix '%s', Größe zwischen %d KB und %d KB.%n",
                this.numFiles, this.startDate.format(FILENAME_DATE_FORMATTER), this.filePrefix, this.minSizeKB, this.maxSizeKB);

        LocalDate currentDate = this.startDate;
        for (int i = 0; i < this.numFiles; i++) {
            long targetSizeBytes = (minSizeBytes == maxSizeBytes)
                    ? minSizeBytes
                    : ThreadLocalRandom.current().nextLong(minSizeBytes, maxSizeBytes + 1);

            String dateString = currentDate.format(FILENAME_DATE_FORMATTER);
            String fileName = String.format("%s-%s.log", this.filePrefix, dateString);
            Path filePath = Paths.get(fileName);

            System.out.printf("Erstelle Datei '%s' für Datum %s (Zielsgröße: %.2f KB)...%n",
                    fileName, dateString, (double) targetSizeBytes / 1024.0);

            try {
                // Übergebe das Datum der Datei, damit die Zeitstempel darin korrekt sind
                generateLogFile(filePath, targetSizeBytes, currentDate);
                System.out.printf("Datei '%s' erfolgreich erstellt (Tatsächliche Größe: %.2f KB).%n",
                        fileName, (double) Files.size(filePath) / 1024.0);
            } catch (IOException e) {
                System.err.printf("Fehler beim Erstellen der Datei '%s': %s%n", fileName, e.getMessage());
            }

            // Gehe zum nächsten Tag für die nächste Datei
            currentDate = currentDate.plusDays(1);
        }
        System.out.println("Log-Generierung abgeschlossen.");
    }

    private void generateLogFile(Path filePath, long targetSizeBytes, LocalDate fileDate) throws IOException {
        long currentSizeBytes = 0;
        // Verwende try-with-resources für automatisches Schließen des Writers
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            while (currentSizeBytes < targetSizeBytes) {
                // Erzeuge Log-Eintrag mit Zeitstempel basierend auf dem Dateidatum
                String logEntry = createLogEntry(fileDate);
                byte[] entryBytes = (logEntry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

                // Stelle sicher, dass die Zielsgröße nicht wesentlich überschritten wird
                if (currentSizeBytes + entryBytes.length > targetSizeBytes && currentSizeBytes > 0) {
                    break;
                }

                writer.write(logEntry);
                writer.newLine();
                currentSizeBytes += entryBytes.length;
            }
        } // Writer wird hier automatisch geschlossen
    }

    // Nimmt jetzt das Datum der Datei entgegen, um realistische Zeitstempel zu erzeugen
    private String createLogEntry(LocalDate fileDate) {
        // Erzeuge eine zufällige Uhrzeit innerhalb des übergebenen Tages
        LocalTime randomTime = LocalTime.ofSecondOfDay(random.nextInt(24 * 60 * 60));
        // Kombiniere Datum und zufällige Zeit, füge zufällige Millisekunden hinzu
        LocalDateTime timestamp = LocalDateTime.of(fileDate, randomTime)
                .plusNanos(random.nextInt(1_000_000_000)); // Nanos für Millisekunden

        String level = getRandomLogLevel();
        String threadName = THREAD_NAMES[random.nextInt(THREAD_NAMES.length)];
        String message = generateRandomMessage(level); // Nachrichtengenerierung bleibt gleich

        return String.format("%s %-5s [%s] %s",
                LOG_ENTRY_TIMESTAMP_FORMATTER.format(timestamp),
                level,
                threadName,
                message);
    }

    private String getRandomLogLevel() {
        int chance = random.nextInt(100);
        if (chance < 5) return "ERROR";    // 5%
        if (chance < 15) return "WARN";     // 10%
        if (chance < 65) return "INFO";     // 50%
        if (chance < 85) return "DEBUG";    // 20%
        return "TRACE";                   // 15%
    }

    private String generateRandomMessage(String level) {
        int messageType = random.nextInt(10);
        boolean likelyNetwork = level.equals("DEBUG") || level.equals("TRACE");
        boolean likelyError = level.equals("ERROR") || level.equals("WARN");

        if ((likelyNetwork && random.nextInt(3) == 0) || (!likelyError && messageType < 2)) {
            return generateNetworkMessage();
        } else if ((likelyError && random.nextInt(2) == 0) || (!likelyNetwork && messageType < 4)) {
            return generateErrorMessage();
        } else {
            return generateGenericMessage(level);
        }
    }

    private String generateNetworkMessage() {
        String[] hosts = {"srv-app-01.internal", "db-cluster.local", "api.external.com", "192.168.1.100", "10.0.5.23", "cache-node-3"};
        int port = random.nextInt(65535 - 1024) + 1024;
        String host = hosts[random.nextInt(hosts.length)];
        boolean success = random.nextBoolean();

        if (success) {
            String[] successActions = {"Successfully connected to", "Established connection with", "Sent data packet to", "Received response from"};
            int latency = random.nextInt(250) + 5; // 5-255 ms Latenz
            return String.format("%s %s:%d (latency: %dms)", successActions[random.nextInt(successActions.length)], host, port, latency);
        } else {
            String[] errorReasons = {"Connection timed out", "Connection refused", "Host not found", "Network unreachable", "SSL handshake failed"};
            return String.format("Failed to connect to %s:%d - %s", host, port, errorReasons[random.nextInt(errorReasons.length)]);
        }
    }

    private String generateErrorMessage() {
        String[] errors = {
                "NullPointerException at com.example.service.UserService:123",
                "FileNotFoundException: /data/config.xml",
                "SQLException: ORA-00942: table or view does not exist",
                "IllegalArgumentException: Invalid user ID format",
                "OutOfMemoryError: Java heap space",
                "ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 4",
                "SecurityException: Access denied for user guest"
        };
        String[] context = {
                "while processing user request " + UUID.randomUUID().toString().substring(0, 8),
                "during batch job execution 'import-data'",
                "in scheduled task 'data-cleanup'",
                "when accessing resource /api/v1/items/" + random.nextInt(10000)
        };
        return String.format("%s %s", errors[random.nextInt(errors.length)], context[random.nextInt(context.length)]);
    }

    private String generateGenericMessage(String level) {
        String userId = "user" + random.nextInt(1000);
        String sessionId = UUID.randomUUID().toString().substring(0, 12);
        String actionId = "task-" + random.nextInt(500);

        switch (level) {
            case "TRACE":
                String[] methods = {"enterMethodX", "exitMethodY", "processInternal", "validateInput"};
                String varName = "var" + random.nextInt(10);
                int value = random.nextInt(1000);
                return String.format("Trace point %d reached in %s. Variable '%s' = %d", random.nextInt(100), methods[random.nextInt(methods.length)], varName, value);
            case "DEBUG":
                String[] debugActions = {"Processing item", "Cache lookup for key", "Parameter validation successful for", "Sending notification for", "Query executed:"};
                String debugDetails = (random.nextBoolean()) ? " item_id=" + actionId : " key=cache_" + userId;
                return String.format("%s%s. Session: %s", debugActions[random.nextInt(debugActions.length)], debugDetails, sessionId);
            case "INFO":
                String[] infoActions = {"User logged in:", "Order placed:", "Configuration loaded successfully from /etc/app.conf.", "Service [PaymentGateway] started.", "Batch job 'report-gen' completed:", "Incoming request handled:"};
                return String.format("%s %s", infoActions[random.nextInt(infoActions.length)], (random.nextBoolean() ? userId : actionId));
            case "WARN":
                String[] warnConditions = {"Configuration key 'db.timeout' not found, using default 30s.", "Disk space low on /var/log (Usage: 92%).", "Deprecated API method '/v1/users' called by client " + hosts[random.nextInt(hosts.length)] , "Request rate limit approaching for user " + userId, "Response time exceeds threshold (550ms) for /api/v2/data"};
                return warnConditions[random.nextInt(warnConditions.length)];
            case "ERROR": // Wird oft von generateErrorMessage() abgedeckt, dies ist ein Fallback
                return "An unexpected condition occurred processing request " + actionId + " for user " + userId;
            default:
                return "Generic log message for ID " + actionId;
        }
    }

    // Hilfsmethode zum Abrufen von Hostnamen (wird in generateNetworkMessage und generateGenericMessage verwendet)
    private static final String[] hosts = {"web-prod-01", "app-staging-02", "db-master.eu-central-1", "10.0.1.55", "ext-api.partner.com"};


    public static void main(String[] args) {
        // Standardwerte
        int numFiles = 5;
        String prefix = "app";
        LocalDate startDate = LocalDate.now().minusDays(numFiles -1); // Standard: Letzte 'numFiles' Tage bis heute
        int minKB = 10;
        int maxKB = 100;

        // --- Argument-Verarbeitung ---
        // Verwendung: java LogGenerator [anzahlTage] [prefix] [startDate YYYY-MM-DD] [minKB] [maxKB]
        try {
            if (args.length >= 1) {
                numFiles = Integer.parseInt(args[0]);
                // Update default start date if numFiles changes
                startDate = LocalDate.now().minusDays(Math.max(0, numFiles - 1));
            }
            if (args.length >= 2) {
                prefix = args[1];
            }
            if (args.length >= 3) {
                try {
                    startDate = LocalDate.parse(args[2], FILENAME_DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    System.err.println("Fehler beim Parsen des Startdatums. Bitte das Format YYYY-MM-DD verwenden.");
                    printUsage();
                    System.exit(1);
                }
            }
            if (args.length >= 4) {
                minKB = Integer.parseInt(args[3]);
            }
            if (args.length >= 5) {
                maxKB = Integer.parseInt(args[4]);
            }
            // Zusätzliche Prüfung nach dem Parsen aller Argumente
            if (args.length > 5) {
                System.err.println("Warnung: Zusätzliche Argumente ignoriert.");
            }

        } catch (NumberFormatException e) {
            System.err.println("Fehler beim Parsen der numerischen Argumente (Anzahl, MinKB, MaxKB).");
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace(); // Für Debugging
            printUsage();
            System.exit(1);
        }

        // Validierung der Werte nach dem Parsen
        try {
            LogGenerator generator = new LogGenerator(numFiles, prefix, startDate, minKB, maxKB);
            generator.generateLogs();
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler bei der Initialisierung des Generators: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("\nVerwendung: java LogGenerator [anzahlTage] [prefix] [startDate YYYY-MM-DD] [minKB] [maxKB]");
        System.err.println("Beispiele:");
        System.err.println("  java LogGenerator                 (Verwendet Standardwerte: 5 Tage bis heute, 'app', 10-100 KB)");
        System.err.println("  java LogGenerator 10 api 2023-10-01 50 200 (Erzeugt 10 Dateien ab 01.10.2023, Prefix 'api', 50-200 KB)");
        System.err.println("  java LogGenerator 7 web 2024-01-15 20 20   (Erzeugt 7 Dateien ab 15.01.2024, Prefix 'web', genau 20 KB)");
    }
}