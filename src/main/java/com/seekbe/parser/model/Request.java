package com.seekbe.parser.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "requests")
public class Request {

    @Id
    private @NonNull
    String id = UUID.randomUUID().toString();

    @JsonProperty
    private String serviceName;

    @JsonProperty
    private String method;

    @JsonProperty
    private String path;

    public static Request of(String serviceName, String method, String path) {
        return Request.builder()
            .id(UUID.randomUUID().toString())
            .serviceName(serviceName)
            .method(method)
            .path(path)
            .build();
    }
}
