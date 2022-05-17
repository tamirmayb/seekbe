package com.seekbe.parser;

import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(value="sb")
public class SBController {

    private static Logger logger = LogManager.getLogger(SBController.class);

    @Autowired
    private ParserProcess process;

    @PostMapping("/process")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<String> process() {
        try {
            return ResponseEntity.ok(Objects.requireNonNull(process.runParser()));
        } catch (Exception e) {
            logger.warn("Caught exception in process: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
}
