package ro.abm.util;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogAppender {


    public static void writeToLogFile(LocalDateTime time, String siteName, String content) throws Exception {
        Path mainDir = Paths.get(System.getProperty("user.dir"));

        Path logsDir = mainDir.resolve("logs");
        Files.createDirectories(logsDir); // ensure logs dir exists

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss");
        String timestamp = time.format(formatter);

        String fileName = siteName + "_" + timestamp + "_" + ".txt";
        Path logFile = logsDir.resolve(fileName);

        try (FileWriter fw = new FileWriter(logFile.toFile(), true)) {
            fw.write(content + "\n");
        }
    }
}
