package com.seekbe.analyzer.errors;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApplicationExceptionAsJSON {
    String url;
    String message;
}