package com.seekbe.parser.component;

import com.seekbe.parser.ParserProcess;
import org.springframework.stereotype.Component;

@Component
public class Parser {
    private final ParserProcess parserProcess;

    public Parser(ParserProcess parserProcess) {
        this.parserProcess = parserProcess;
    }
}
