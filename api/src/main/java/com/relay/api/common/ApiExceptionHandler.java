package com.relay.api.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handle(ApiException exception) {
        return problem(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DataIntegrityViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "CSV contains a duplicate recipientId or invalid data.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "CSV exceeds the 200 MB upload limit.");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}
