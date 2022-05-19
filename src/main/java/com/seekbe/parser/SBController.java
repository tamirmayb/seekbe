package com.seekbe.parser;

import com.seekbe.parser.dto.BusyDTO;
import com.seekbe.parser.model.Method;
import com.seekbe.parser.model.Request;
import com.seekbe.parser.services.RequestService;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value="sb")
public class SBController {

    private static Logger logger = LogManager.getLogger(SBController.class);

    @Autowired
    RequestService requestService;

    @PostMapping("/startParser")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<String> startParser() {
        try {
            return ResponseEntity.ok(Objects.requireNonNull(requestService.startParser()));
        } catch (Exception e) {
            logger.warn("Caught exception in process: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stats")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<List<Request>> getStats(@RequestParam String serviceName, @RequestParam Method method) {
        try {
            return ResponseEntity.ok(requestService.findByServiceNameAndMethod(serviceName, method));
        } catch (Exception e) {
            logger.warn("Caught exception in getStats: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/busy")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<List<BusyDTO>> getBusy(@RequestParam int limit) {
        try {
            return ResponseEntity.ok(requestService.getBusy(limit));
        } catch (Exception e) {
            logger.warn("Caught exception in getBusy: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<String> deleteService(@RequestParam String serviceName) {
        try {
            return ResponseEntity.ok(requestService.deleteService(serviceName));
        } catch (Exception e) {
            logger.warn("Caught exception in deleteService: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
}
