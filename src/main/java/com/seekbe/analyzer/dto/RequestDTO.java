package com.seekbe.analyzer.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class RequestDTO {
    private String serviceName;
    private String method;
    private String uri;

}
