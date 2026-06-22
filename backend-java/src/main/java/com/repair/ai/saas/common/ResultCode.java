package com.repair.ai.saas.common;

public final class ResultCode {

    private ResultCode() {}

    public static final String SUCCESS = "SUCCESS";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
}
