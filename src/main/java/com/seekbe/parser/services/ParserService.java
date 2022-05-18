package com.seekbe.parser.services;

import com.seekbe.parser.Utils;
import com.seekbe.parser.runnables.ParserTask;
import com.seekbe.parser.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ParserService {
    @Autowired
    private RequestRepository requestRepository;

    @Value("${parser.workers}")
    private Integer workers;

    @Value("${parser.sources.path}")
    private String path;

    @Value("${parser.backup.path}")
    private String backupPath;

    @Value("${parser.regex.path}")
    private String pathToRegex;


    public String runParser() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(workers);
        Utils.checkBackupDir(backupPath); //create backup directory automatically if not exists.

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
                .forEach(path -> {
                    ParserTask task = new ParserTask(requestRepository, path.toString(), pathToRegex, backupPath);
                    executor.execute(task);
                });
            executor.shutdown();
            return "done";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
