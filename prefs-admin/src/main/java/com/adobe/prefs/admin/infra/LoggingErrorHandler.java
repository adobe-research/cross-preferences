package com.adobe.prefs.admin.infra;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

public class LoggingErrorHandler extends ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoggingErrorHandler.class);
    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
        Throwable t = (Throwable)request.getAttribute("javax.servlet.error.exception");
        if (t != null) {
            logger.error(message, t);
        } else {
            logger.error("Status code {}: {}", code, message);
        }
    }

}
