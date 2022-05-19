package com.seekbe.parser.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class requestDTO {
    private String serviceName;
    private String method;
    private String uri;

}
