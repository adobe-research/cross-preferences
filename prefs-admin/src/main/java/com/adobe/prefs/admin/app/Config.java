package com.adobe.prefs.admin.app;

import com.adobe.prefs.admin.infra.HtmlMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2CollectionHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.List;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

@Configuration
@EnableWebMvc
@EnableHypermediaSupport(type = HypermediaType.HAL)
public class Config extends WebMvcConfigurerAdapter {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
      configurer
              .favorPathExtension(false)
              .favorParameter(true)
              .ignoreAcceptHeader(false)
              .defaultContentType(MediaType.APPLICATION_JSON)
              .mediaType("json", new MediaType("application", "hal+json"))
              .mediaType("html", MediaType.TEXT_HTML);
    }
    @Bean
    UserPrefsController userPrefsController() {
        return new UserPrefsController();
    }

    @Bean
    SystemPrefsController systemPrefsController() {
        return new SystemPrefsController();
    }

    @Bean
    HomeController homeController() {
        return new HomeController();
    }

    @Bean
    MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new Jaxb2RootElementHttpMessageConverter());
        converters.add(new Jaxb2CollectionHttpMessageConverter<>());
        converters.add(new HtmlMessageConverter());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor webContentInterceptor = new WebContentInterceptor();
        webContentInterceptor.setCacheSeconds(0);
        webContentInterceptor.setUseExpiresHeader(true);
        webContentInterceptor.setUseCacheControlHeader(true);
        webContentInterceptor.setUseCacheControlNoStore(true);
        registry.addInterceptor(new WebContentInterceptor());
    }

}
