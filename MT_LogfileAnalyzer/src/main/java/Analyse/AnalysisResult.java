package Analyse;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Hilfs-Datenklasse, die das Ergebnis einer einzelnen Log-Analyse enthält:
 *  - Datei-Pfad
 *  - Zählung der LogLevel
 *  - Liste aller WARN- und ERROR-Zeilen
 *  - Zählung bekannter Fehler-Arten (NullPointerException usw.)
 */
public class AnalysisResult {
    private final Path logFile;
    private final Map<String, Integer> levelCounts;
    private final List<String> warnErrorLines;
    private final Map<String, Integer> errorTypeCounts;

    public AnalysisResult(Path logFile,
                          Map<String, Integer> levelCounts,
                          List<String> warnErrorLines,
                          Map<String, Integer> errorTypeCounts) {
        this.logFile = logFile;
        this.levelCounts = levelCounts;
        this.warnErrorLines = warnErrorLines;
        this.errorTypeCounts = errorTypeCounts;
    }

    public Path getLogFile() {
        return logFile;
    }

    public Map<String, Integer> getLevelCounts() {
        return levelCounts;
    }

    public List<String> getWarnErrorLines() {
        return warnErrorLines;
    }

    public Map<String, Integer> getErrorTypeCounts() {
        return errorTypeCounts;
    }
}