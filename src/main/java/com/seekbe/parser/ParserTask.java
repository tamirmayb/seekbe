package com.seekbe.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class ParserTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ParserTask.class);

    @Getter
    @Setter
    private String path;

    @Override
    public void run() {
        log.info("starting path = " + path + "thread = " + Thread.currentThread().getName());
    }
}
