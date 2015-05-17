package com.adobe.prefs.admin.infra;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
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

    private static final String TEMPLATE = "com/adobe/prefs/admin/infra/prefs-template.ftl";

    final Template template;

    public HtmlMessageConverter() {
        super(MediaType.TEXT_HTML);
        Configuration config = new Configuration();
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setClassForTemplateLoading(getClass(), "/");
        try {
            template = config.getTemplate(TEMPLATE);
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
    protected void writeInternal(ResourceSupport resource, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final Writer writer = new OutputStreamWriter(outputMessage.getBody());
        try {
            template.process(resource, writer);
        } catch (TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
