package com.adobe.prefs.admin.infra;

import com.adobe.prefs.admin.core.HomeResource;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by ciudatu on 7/26/14.
 */
public class HtmlMessageConverter extends AbstractHttpMessageConverter<ResourceSupport> {

    private static final String HOME_TEMPLATE = "com/adobe/prefs/admin/infra/home-template.ftl";
    private static final String PREFS_TEMPLATE = "com/adobe/prefs/admin/infra/prefs-template.ftl";

    final Template homeTemplate;
    final Template prefsTemplate;

    public HtmlMessageConverter() {
        super(MediaType.TEXT_HTML);
        Configuration config = new Configuration();
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setClassForTemplateLoading(getClass(), "/");
        try {
            homeTemplate = config.getTemplate(HOME_TEMPLATE);
            prefsTemplate = config.getTemplate(PREFS_TEMPLATE);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ResourceSupport.class.isAssignableFrom(clazz);
    }

    @Override
    protected ResourceSupport readInternal(Class<? extends ResourceSupport> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(ResourceSupport resourceSupport, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final Writer writer = new OutputStreamWriter(outputMessage.getBody());
        try {
            if (resourceSupport instanceof HomeResource) {
                homeTemplate.process(resourceSupport, writer);
            } else {
                prefsTemplate.process(resourceSupport, writer);
            }
        } catch (TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
