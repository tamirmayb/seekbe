package com.seekbe.analyzer.runnables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seekbe.analyzer.Utils;
import com.seekbe.analyzer.config.MongoConfig;
import com.seekbe.analyzer.dto.RequestDTO;
import com.seekbe.analyzer.model.Method;
import com.seekbe.analyzer.model.Request;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ParserTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ParserTask.class);

    private static final String INDEX = "_index";
    private static final String SOURCE = "_source";
    private static final String LAYERS = "layers";
    private static final String HTTP = "http";
    private static final String REQUEST_METHOD = "http.request.method";
    private static final String REQUEST_URI = "http.request.uri";

    private final MongoConfig mongo;
    private final String path;
    private final String pathToRegex;
    private final String backupPath;
    private final int dbBatchSize;

    @Override
    public void run() {
        log.info("parsing file = " + path + " thread = " + Thread.currentThread().getName());

        try {
            MongoOperations mongoOps = new MongoTemplate(mongo.mongoClient(), mongo.getDatabaseName());
            List<Request> requestListBatch = new ArrayList<>();

            JSONParser parser = new JSONParser();
            ObjectMapper mapper = new ObjectMapper();
            JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(path));

            for (Object o : jsonArray) {
                JSONObject input = (JSONObject) o;
                String index = (String) input.get(INDEX);

                try {
                    Map<String, Object> map = parseJsonInput(mapper, input);

                    for (Object value : map.values()) {
                        if (value instanceof Map) {
                            Map<String, String> requestMap = (Map) value;

                            if(requestMap.containsKey(REQUEST_METHOD) && requestMap.containsKey(REQUEST_URI)) {
                                RequestDTO dto = requestToDTO(requestMap.get(REQUEST_METHOD),
                                        requestMap.get(REQUEST_URI));

                                if (dto != null) {
                                    Method method = EnumUtils.getEnum(Method.class, dto.getMethod());
                                    requestListBatch.add(Request.of(dto.getServiceName(), method, dto.getUri()));
                                    break;
                                }
                            }
                        }
                    }
                    if(requestListBatch.size() > dbBatchSize) {
                        saveMongoBatch(mongoOps, requestListBatch);
                        requestListBatch.clear();
                    }

                } catch (Exception innerEx) {
                    log.error("Error parsing request with index: " + index + " in file: " + path + " " + innerEx.getMessage());
                }
            }
            if(requestListBatch.size() > 0) {
                saveMongoBatch(mongoOps, requestListBatch);
                requestListBatch.clear();
            }
            String backupFileName = backupPath + path.substring(path.lastIndexOf("/"));
            Utils.moveFile(path, backupFileName);
            log.info("Processing file " + path + " is done.");

        } catch (Exception e) {
            log.error("Error parsing input " + path + " " + e.getMessage());
        }
    }

    private Map<String, Object> parseJsonInput(ObjectMapper mapper, JSONObject root) throws JsonProcessingException {
        JSONObject source = (JSONObject) root.get(SOURCE);
        JSONObject layers = (JSONObject) source.get(LAYERS);
        JSONObject http = (JSONObject) layers.get(HTTP);
        Map<String, Object> map = mapper.readValue(http.toJSONString(), Map.class);
        return map;
    }

    private void saveMongoBatch(MongoOperations mongoOps, List<Request> requestListBatch) {
        BulkOperations bulkOps = mongoOps.bulkOps(BulkOperations.BulkMode.UNORDERED, Request.class);
        bulkOps.insert(requestListBatch);
        bulkOps.execute();
    }

    private RequestDTO requestToDTO(String requestMethod, String requestUri) {
        try {
            Map<String,String> map = new ObjectMapper().readValue(new FileReader(pathToRegex), HashMap.class);
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String regex = entry.getKey();
                String serviceName = entry.getValue();
                if (Pattern.compile(regex).matcher(requestUri).matches()) {
                    return new RequestDTO(serviceName, requestMethod, requestUri);
                }
            }
        } catch (IOException e) {
            log.error("Error in requestToDTO " + requestUri + " " + e.getMessage());
        }
        return null;
    }
}
