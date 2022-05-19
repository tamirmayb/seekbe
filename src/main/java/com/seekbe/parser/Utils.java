package com.seekbe.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    public static Path moveFile(String src, String dest) throws IOException {
        Path result = Files.move(Paths.get(src), Paths.get(dest));
        return result;
    }

    public static void checkBackupDir(String path) {
        File directory = new File(path);
        if (! directory.exists()) {
            directory.mkdir();
        }
    }

    public static void checkRegexFile(String path) throws FileNotFoundException {
        File directory = new File(path);
        if (! directory.exists()) {
            throw new FileNotFoundException(path + " not found.");
        }
    }
}
