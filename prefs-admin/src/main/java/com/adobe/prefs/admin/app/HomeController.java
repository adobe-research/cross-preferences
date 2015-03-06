package com.adobe.prefs.admin.app;

import com.adobe.prefs.admin.core.HomeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    public static final String ROOT = "/v1";

    @RequestMapping("/")
    String redirect() {
        return "redirect:" + ROOT;
    }

    @RequestMapping(value = HomeController.ROOT, method = RequestMethod.GET)
    @ResponseBody
    ResourceSupport home() {
        final HomeResource resource = new HomeResource(Preferences.systemRoot().getClass().getName());
        resource.add(new Link(ROOT + "/usr/", "usr"));
        resource.add(new Link(ROOT + "/sys/", "sys"));
        return resource;
    }

    @RequestMapping(value = HomeController.ROOT, method = RequestMethod.POST,
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void importPreferences(InputStream in) throws IOException, InvalidPreferencesFormatException {
        logger.info("Importing preferences from file...");
        Preferences.importPreferences(in);
        logger.info("Preferences import succeeded");
    }

    @RequestMapping(value = HomeController.ROOT, method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void importPreferences(@RequestParam MultipartFile file) throws IOException, InvalidPreferencesFormatException {
        importPreferences(file.getInputStream());
    }

}
