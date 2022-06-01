package com.seekbe.analyzer;

import com.seekbe.analyzer.dto.BusyDTO;
import com.seekbe.analyzer.model.Method;
import com.seekbe.analyzer.model.Request;
import com.seekbe.analyzer.services.AnalyzerService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@AllArgsConstructor
@RequestMapping(value="sb")
public class SBController {

    private final AnalyzerService analyzerService;

    @PostMapping("/startParser")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<String> startParser() {
        return ResponseEntity.ok(Objects.requireNonNull(analyzerService.startParser()));
    }

    @GetMapping("/stats")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<List<Request>> getStats(@RequestParam String serviceName, @RequestParam Method method) {
        return ResponseEntity.ok(analyzerService.findByServiceNameAndMethod(serviceName, method));
    }

    @GetMapping("/busy")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<List<BusyDTO>> getBusy(@RequestParam int limit) {
        return ResponseEntity.ok(analyzerService.getBusy(limit));
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "",  notes = "")
    public ResponseEntity<String> deleteService(@RequestParam String serviceName) {
        return ResponseEntity.ok(analyzerService.deleteService(serviceName));
    }
}
