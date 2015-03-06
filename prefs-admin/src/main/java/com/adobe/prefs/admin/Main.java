package com.adobe.prefs.admin;

import com.adobe.prefs.admin.app.Config;
import com.adobe.prefs.admin.infra.LoggingErrorHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import java.util.EnumSet;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final int DEFAULT_PORT = 8910;

    public static void main(String[] args) throws Exception {
        // can choose a custom port as the first main argument
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("The first argument is not a port number: {}. Using default port: {}", args[0], DEFAULT_PORT);
            }
        }

        // Jetty customization for removing the server header
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(true);

        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        final ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

        // Spring dispatcher servlet
        final DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setContextClass(AnnotationConfigWebApplicationContext.class);
        dispatcherServlet.setContextConfigLocation(Config.class.getName());
        final ServletHolder sh = new ServletHolder(dispatcherServlet);
        sh.getRegistration().setMultipartConfig(new MultipartConfigElement("/var/tmp"));
        sch.addServlet(sh, "/");

        // filter for allowing put and delete methods to be simulated via post
        sch.addFilter(HiddenHttpMethodFilter.class, "/*", EnumSet.noneOf(DispatcherType.class));

        // filter that enforces UTF-8 encoding
        final CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        sch.addFilter(new FilterHolder(characterEncodingFilter), "/*", EnumSet.noneOf(DispatcherType.class));

        // get rid of the Jetty HTML error pages
        sch.setErrorHandler(new LoggingErrorHandler());

        server.setHandler(sch);
        server.start();
        server.join();
    }

}
