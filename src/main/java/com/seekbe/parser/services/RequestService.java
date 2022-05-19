package com.seekbe.parser.services;

import com.google.common.collect.Lists;
import com.seekbe.parser.config.MongoConfig;
import com.seekbe.parser.dto.BusyDTO;
import com.seekbe.parser.model.Method;
import com.seekbe.parser.model.Request;
import com.seekbe.parser.repositories.RequestRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Service
public class RequestService {
    private static final Logger log = LogManager.getLogger(RequestService.class);

    private static final String SERVICE_NAME = "serviceName";

    @Autowired
    private MongoConfig mongo;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ParserService process;

    @Value("${parser.workers}")
    private Integer workers;

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
        mongoOps.findAllAndRemove(query, Request.class);
        return "done";
    }
}
