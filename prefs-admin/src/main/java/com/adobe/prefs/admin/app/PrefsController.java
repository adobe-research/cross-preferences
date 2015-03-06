package com.adobe.prefs.admin.app;

import com.adobe.prefs.admin.core.NodeResource;
import com.adobe.prefs.admin.core.Paths;
import com.adobe.prefs.admin.core.PrefResource;
import com.adobe.prefs.admin.core.UrlIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.springframework.http.HttpStatus.*;

@Controller
abstract class PrefsController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String realm;
    private final Preferences root;

    protected PrefsController(String realm, Preferences root) {
        this.root = root;
        this.realm = realm;
    }

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    @ResponseBody
    ResourceSupport getPreference(HttpServletRequest request) throws BackingStoreException {
        final PrefSpec prefSpec = new PrefSpec(request);
        if (!root.nodeExists(prefSpec.nodePath)) {
            throw new ResourceNotFoundException(prefSpec.nodePath);
        }
        final Preferences prefs = root.node(prefSpec.nodePath);
        if (prefSpec.key == null) {
            return new NodeResource(realm, prefs);
        } else {
            if (! Arrays.asList(prefs.keys()).contains(prefSpec.key) ) {
                throw new ResourceNotFoundException();
            }
            return new PrefResource(realm, prefs, prefSpec.key, true);
        }
    }


    @RequestMapping(value = "/**", method = RequestMethod.PUT)
    @ResponseBody
    ResponseEntity<Void> setPreference(HttpServletRequest request, @RequestParam(required = false) String value) throws BackingStoreException {
        final PrefSpec prefSpec = new PrefSpec(request);
        Preferences prefs = root.node(prefSpec.nodePath);
        if (prefSpec.key != null) {
            if (value == null) {
                throw new NoValueException(prefSpec.key);
            }
            try {
                prefs.put(prefSpec.key, URLDecoder.decode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("WTF-8", e);
            }
        }
        return seeOtherResponse(prefSpec);
    }

    @RequestMapping(value = "/**", method = RequestMethod.DELETE)
    ResponseEntity<Void> removePreference(HttpServletRequest request) throws BackingStoreException {
        final PrefSpec prefSpec = new PrefSpec(request);
        if (! root.nodeExists(prefSpec.nodePath) ) {
            throw new ResourceNotFoundException(prefSpec.nodePath);
        }
        Preferences prefs = root.node(prefSpec.nodePath);
        if (prefSpec.key != null) {
            if (! Arrays.asList(prefs.keys()).contains(prefSpec.key) ) {
                throw new ResourceNotFoundException(prefSpec.key);
            }
            prefs.remove(prefSpec.key);
        } else {
            prefs.removeNode();
        }
        return seeOtherResponse(prefSpec);
    }

    @RequestMapping(value = "/**", method = RequestMethod.GET, params = "export", produces = MediaType.APPLICATION_XML_VALUE)
    void export(HttpServletRequest request,
                HttpServletResponse response,
                @RequestParam String[] export) throws BackingStoreException, IOException {
        final Set<String> exportOptions = new HashSet<>(Arrays.asList(export));
        final PrefSpec prefSpec = new PrefSpec(request);
        if (!root.nodeExists(prefSpec.nodePath)) {
            throw new ResourceNotFoundException(prefSpec.nodePath);
        }
        Preferences prefs = root.node(prefSpec.nodePath);
        final boolean shallow = exportOptions.contains("shallow");
        response.setContentType("application/xml");
        if (exportOptions.contains("file")) {
            response.setHeader("Content-Disposition",
                    String.format("attachment; filename=\"prefs_%s%s_%s.xml\"",
                            prefs.isUserNode() ? "usr" : "sys",
                            prefs.absolutePath().length() > 1 ? prefs.absolutePath().replace('/', '-') : "",
                            shallow ? "shallow" : "deep"));
        }
        if (shallow) {
            prefs.exportNode(response.getOutputStream());
        } else {
            prefs.exportSubtree(response.getOutputStream());
        }
    }

    ResponseEntity<Void> seeOtherResponse(PrefSpec prefSpec) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", Paths.parent(Paths.path(realm, prefSpec.nodePath, prefSpec.key)));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public void badRequest(IllegalArgumentException e) {
        logger.error(BAD_REQUEST.getReasonPhrase(), e);
    }

    @ExceptionHandler
    @ResponseStatus(SERVICE_UNAVAILABLE)
    public void serviceUnavailable(IllegalStateException e) {
        logger.error(SERVICE_UNAVAILABLE.getReasonPhrase(), e);
    }

    @ExceptionHandler
    @ResponseStatus(NOT_IMPLEMENTED)
    public void notImplemented(UnsupportedOperationException e) {
        logger.error(NOT_IMPLEMENTED.getReasonPhrase(), e);
    }

    @ExceptionHandler
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public void internalServerError(Exception e) {
        logger.error(INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
    }

    class PrefSpec {
        final String nodePath;
        final String key;

        PrefSpec(HttpServletRequest request) {
          final String requestPath = UrlIO.DECODER.apply(request.getRequestURI());
          if (requestPath != null && requestPath.startsWith(realm)) {
                String path = requestPath.substring(realm.length());
                if (path.isEmpty() || !path.startsWith("/")) {
                    path = "/" + path;
                }
                final int lastSlash = path.lastIndexOf('/');
                nodePath = path.substring(0, lastSlash);
                key = lastSlash == path.length() - 1 ? null : path.substring(lastSlash + 1, path.length());
            } else {
                throw new IllegalArgumentException();
            }
        }

    }

}
