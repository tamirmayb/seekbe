package com.seekbe.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    public static Path moveFile(String src, String dest) throws IOException {
        Path result = Files.move(Paths.get(src), Paths.get(dest));
        return result;
    }

    public static void checkBackupDir(String backupPath) {
        File directory = new File(backupPath);
        if (! directory.exists()) {
            directory.mkdir();
        }
    }
}
