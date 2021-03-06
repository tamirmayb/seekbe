package com.seekbe.analyzer.repositories;

import com.seekbe.analyzer.model.Method;
import com.seekbe.analyzer.model.Request;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends MongoRepository<Request, String> {
    Optional<List<Request>> findByServiceNameAndMethod(String serviceName, Method method);
}

