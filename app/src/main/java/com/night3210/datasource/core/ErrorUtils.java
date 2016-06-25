package com.night3210.datasource.core;

/**
 * Created by Developer on 2/12/2016.
 */
public class ErrorUtils {

    private ErrorUtils() {
        throw new AssertionError("non-instantiable class");
    }

    private static final String ERROR_CODE = "code";

    private static final String ERROR_MESSAGE = "message";

    public static final int DEFAULT_ERROR_CODE = -1234;

    public static Exception createWrongServerDataException(String reason) {
        return new Exception(reason);
    }
}
