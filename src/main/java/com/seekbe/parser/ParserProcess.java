package com.seekbe.parser;

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
public class ParserProcess {

    @Value("${parser.workers}")
    private String workers;

    public String runParser() {
        System.out.println("workers = " + workers);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/requests"))) {
            paths
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
                .forEach(path -> {
                    ParserTask task = new ParserTask(path.toString());
                    executor.execute(task);
                });
            return "done";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
