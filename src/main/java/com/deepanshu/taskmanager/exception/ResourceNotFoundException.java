package com.deepanshu.taskmanager.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " not found with " + field + " = '" + value + "'");
    }
}
