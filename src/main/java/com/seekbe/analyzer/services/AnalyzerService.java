package com.seekbe.analyzer.services;

import com.google.common.collect.Lists;
import com.seekbe.analyzer.config.MongoConfig;
import com.seekbe.analyzer.dto.BusyDTO;
import com.seekbe.analyzer.model.Method;
import com.seekbe.analyzer.model.Request;
import com.seekbe.analyzer.repositories.RequestRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AnalyzerService {
    private static final Logger log = LogManager.getLogger(AnalyzerService.class);

    private static final String SERVICE_NAME = "serviceName";

    private final MongoConfig mongo;

    private final RequestRepository requestRepository;

    private final ParserService process;

    public List<BusyDTO> getBusy(int limit) {
        log.info("starting getBusy");

        List<BusyDTO> busy = new ArrayList<>();

        List<Map.Entry<String, Long>> results = requestRepository
                .findAll()
                .stream()
                .collect(Collectors
                        .groupingBy(Request::getServiceName,
                                Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Collections
                        .reverseOrder(Map.Entry
                                .comparingByValue()))
                .limit(limit)
                .collect(Collectors.toList());

        results.forEach(r-> busy.add(new BusyDTO(r.getKey(), r.getValue())));
        return busy;
    }

    public List<Request> findByServiceNameAndMethod(String serviceName, Method method) {
        List<Request> requests = requestRepository.findByServiceNameAndMethod(serviceName, method)
                .orElse(Lists.newArrayList());
        log.info("findByServiceNameAndMethod Request found = " + requests.size());
        return requests;
    }

    public String startParser() {
        return process.runParser();
    }

    public String deleteService(String serviceName) {
        log.info("deleting all entries with service name = " + serviceName);
        MongoOperations mongoOps = new MongoTemplate(mongo.mongoClient(), mongo.getDatabaseName());
        Query query = new Query();
        query.addCriteria(Criteria.where(SERVICE_NAME).is(serviceName));
        mongoOps.remove(query, Request.class);
        return "done";
    }
}
