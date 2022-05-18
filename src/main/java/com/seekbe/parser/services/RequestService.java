package com.seekbe.parser.services;

import com.google.common.collect.Lists;
import com.seekbe.parser.dto.BusyDTO;
import com.seekbe.parser.model.Request;
import com.seekbe.parser.repositories.RequestRepository;
import com.seekbe.parser.runnables.DeleteRequestTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class RequestService {
    private static final Logger logger = LogManager.getLogger(RequestService.class);

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    private ParserService process;

    @Value("${parser.workers}")
    private Integer workers;

    public List<BusyDTO> getBusy(int limit) {
        List<BusyDTO> busy = new ArrayList<>();

        List<Map.Entry<String, Long>> results = requestRepository.findAll()
                .stream()
                .collect(Collectors
                        .groupingBy(Request::getServiceName, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(limit)
                .collect(Collectors.toList());

        results.forEach(r-> busy.add(new BusyDTO(r.getKey(), r.getValue())));
        return busy;
    }

    public List<Request> findByServiceNameAndMethod(String serviceName, String method) {
        return requestRepository.findByServiceNameAndMethod(serviceName, method)
                .orElse(Lists.newArrayList());
    }

    public String startParser() {
        return process.runParser();
    }

    public String deleteService(String serviceName) {
        List<Request> byServiceName = requestRepository
                .findByServiceName(serviceName)
                .orElse(Lists.newArrayList());
        if (byServiceName.size() > 0) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(workers);
            byServiceName.forEach(s-> {
                DeleteRequestTask deleteRequestTask = new DeleteRequestTask(requestRepository, s.getId());
                executor.execute(deleteRequestTask);
            });
            executor.shutdown();
        }
        return "done";
    }
}
