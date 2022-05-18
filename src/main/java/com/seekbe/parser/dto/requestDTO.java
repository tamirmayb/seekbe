package com.seekbe.parser.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class requestDTO {

    @Getter
    @Setter
    private String serviceName;

    @Getter
    @Setter
    private String method;

    @Getter
    @Setter
    private String uri;

}
