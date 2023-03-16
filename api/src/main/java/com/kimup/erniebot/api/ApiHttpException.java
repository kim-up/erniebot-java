package com.kimup.erniebot.api;

public class ApiHttpException extends RuntimeException {

    public ApiHttpException(ApiError error, Exception parent, int statusCode) {

    }
}
