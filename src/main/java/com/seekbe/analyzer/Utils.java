package com.seekbe.analyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {

    public static void moveFile(String src, String dest) throws IOException {
        Files.move(Paths.get(src), Paths.get(dest));
    }

    public static void checkBackupDir(String path) throws IllegalArgumentException {
        File directory = new File(path);
        if (! directory.exists()) {
            if (!directory.mkdir()) {
                throw new IllegalArgumentException("Cannot create directory in " + path);
            }
        }
    }

    public static void checkRegexFile(String path) throws FileNotFoundException {
        File file = new File(path);
        if (! file.exists()) {
            throw new FileNotFoundException(path + " not found.");
        }
    }
}
