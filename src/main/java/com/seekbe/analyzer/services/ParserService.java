package com.seekbe.analyzer.services;

import com.seekbe.analyzer.Utils;
import com.seekbe.analyzer.config.MongoConfig;
import com.seekbe.analyzer.runnables.ParserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    @Autowired
    private MongoConfig mongo;

    @Value("${parser.workers}")
    private Integer workers;

    @Value("${parser.sources.path}")
    private String path;

    @Value("${parser.backup.path}")
    private String backupPath;

    @Value("${parser.regex.path}")
    private String pathToRegex;

    @Value("${db.batch.size}")
    private Integer batchSize;


    public String runParser() {
        log.info("Parsing started, workers = " + workers);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(workers);

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            Utils.checkBackupDir(backupPath); //create backup directory automatically if not exists.
            Utils.checkRegexFile(pathToRegex);
            paths
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
                .forEach(path -> {
                    ParserTask task = new ParserTask(mongo, path.toString(), pathToRegex, backupPath, batchSize);
                    executor.execute(task);
                });
            return "done";

        } catch (Exception e) {
            log.error("error in runParser " + e.getMessage());
        }
        finally {
            executor.shutdown();
        }
        return null;
    }
}
