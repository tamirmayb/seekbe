package com.seekbe.analyzer.errors;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;

import static java.util.Objects.nonNull;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    ApplicationExceptionAsJSON entityNotFoundException(@NonNull HttpServletRequest request,
                                                       @NonNull Exception ex) {
        Preconditions.checkArgument(nonNull(request.getRequestURI()));
        Preconditions.checkArgument(nonNull(ex.getLocalizedMessage()));

        return ApplicationExceptionAsJSON.builder()
                .url(request.getRequestURI())
                .message(ex.getLocalizedMessage())
                .build();
    }
}