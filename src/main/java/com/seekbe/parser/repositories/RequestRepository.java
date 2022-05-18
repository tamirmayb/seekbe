package com.seekbe.parser.repositories;

import com.seekbe.parser.model.Request;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends MongoRepository<Request, String> {
    Optional<List<Request>> findByServiceNameAndMethod(String serviceName, String method);
    Optional<List<Request>> findByServiceName(String serviceName);
}

