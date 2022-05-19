package com.seekbe.parser.runnables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seekbe.parser.Utils;
import com.seekbe.parser.config.MongoConfig;
import com.seekbe.parser.dto.requestDTO;
import com.seekbe.parser.model.Method;
import com.seekbe.parser.model.Request;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@AllArgsConstructor
public class ParserTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ParserTask.class);

    private static final String NO_VALUE = "no_value";
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
            List<Request> requestListBatch = Collections.synchronizedList(new ArrayList<>());

            JSONParser parser = new JSONParser();
            ObjectMapper mapper = new ObjectMapper();
            JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(path));

            for (Object o : jsonArray) {
                JSONObject root = (JSONObject) o;
                String index = (String) root.get(INDEX);

                try {
                    JSONObject source = (JSONObject) root.get(SOURCE);
                    JSONObject layers = (JSONObject) source.get(LAYERS);
                    JSONObject http = (JSONObject) layers.get(HTTP);
                    Map<String, Object> map = mapper.readValue(http.toJSONString(), Map.class);

                    for (Object value : map.values()) {
                        if (value instanceof Map) {
                            Map<String, String> requestMap = (Map) value;
                            String requestMethod = requestMap.getOrDefault(REQUEST_METHOD, NO_VALUE);
                            String requestUri = requestMap.getOrDefault(REQUEST_URI, NO_VALUE);
                            if (!requestMethod.equals(NO_VALUE) && !requestUri.equals(NO_VALUE)) {
                                requestDTO dto = requestToDTO(requestMethod, requestUri);
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

    private void saveMongoBatch(MongoOperations mongoOps, List<Request> requestListBatch) {
        BulkOperations bulkOps = mongoOps.bulkOps(BulkOperations.BulkMode.UNORDERED, Request.class);
        bulkOps.insert(requestListBatch);
        bulkOps.execute();
    }

    private requestDTO requestToDTO(String requestMethod, String requestUri) {
        JSONParser parser = new JSONParser();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(pathToRegex));
            Map<String, String> map = mapper.readValue(jsonObject.toJSONString(), Map.class);
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String regex = entry.getKey();
                String serviceName = entry.getValue();
                if (Pattern.compile(regex).matcher(requestUri).matches()) {
                    return new requestDTO(serviceName, requestMethod, requestUri);
                }
            }
        } catch (IOException | ParseException e) {
            log.error("Error in requestToDTO " + requestUri + " " + e.getMessage());
        }
        return null;
    }
}
