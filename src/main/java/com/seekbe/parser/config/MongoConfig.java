package com.seekbe.parser.config;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Collection;
import java.util.Collections;

@Configuration
@EnableMongoRepositories(basePackages = "com.seekbe.parser.repositories")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.user}")
    private String dbUser;

    @Value("${db.pass}")
    private String pass;

    @Value("${db.name}")
    private String dbName;

    @Override
    public String getDatabaseName() {
        return dbName;
    }

    @Override
    public MongoClient mongoClient() {
        String dbCredentials = String.format(dbUrl, dbUser, pass);
        ConnectionString connectionString = new ConnectionString(dbCredentials);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @Override
    public Collection getMappingBasePackages() {
        return Collections.singleton("com.seekbe.parser");
    }
}