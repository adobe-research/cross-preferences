package com.adobe.prefs.admin.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NoValueException extends RuntimeException {
    public NoValueException() {
    }

    public NoValueException(final String message) {
        super(message);
    }

    public NoValueException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NoValueException(final Throwable cause) {
        super(cause);
    }

    public NoValueException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
