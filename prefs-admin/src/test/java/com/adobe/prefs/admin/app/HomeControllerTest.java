package com.adobe.prefs.admin.app;

import com.adobe.prefs.admin.infra.HtmlMessageConverter;
import com.google.common.base.Charsets;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2CollectionHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.nio.file.Files.readAllBytes;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class HomeControllerTest {

    final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new HomeController()
    ).setMessageConverters(
            new HtmlMessageConverter(),
            new Jaxb2RootElementHttpMessageConverter(),
            new Jaxb2CollectionHttpMessageConverter<>(),
            new MappingJackson2HttpMessageConverter()
    ).build();

    @Test
    public void thereShouldBeNoPlaceLikeV1() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is(302))
                .andExpect(header().string("Location", "/v1"));
    }

    @Test
    public void shouldProduceAlmostHalJson() throws Exception {
        mvc.perform(get("/v1").accept(APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(header().string("Content-Type",
                        new MediaType("application", "json", Charsets.UTF_8).toString()))
                .andExpect(jsonPath("links").exists())
                .andExpect(jsonPath("links[0].href").value("/v1/usr/"))
                .andExpect(jsonPath("links[1].href").value("/v1/sys/"));
    }

    @Test
    public void shouldProduceAlmostHalXml() throws Exception {
        mvc.perform(get("/v1").accept(APPLICATION_XML))
                .andExpect(status().is(200))
                .andExpect(header().string("Content-Type", APPLICATION_XML_VALUE))
                .andExpect(xpath("/resource/link[@rel='usr']").exists())
                .andExpect(xpath("/resource/link[@rel='usr']/@href").string("/v1/usr/"))
                .andExpect(xpath("/resource/link[@rel='sys']/@href").string("/v1/sys/"));
    }

    @Test void shouldProduceHtml() throws Exception {
        mvc.perform(get("/v1").accept(TEXT_HTML))
                .andExpect((status().is(200)));
    }

    @Test
    public void shouldUploadInlineXml() throws Exception {
        final Path path = Paths.get(ClassLoader.getSystemResource("usr.xml").toURI());
        mvc.perform(post("/v1").contentType(APPLICATION_XML).content(readAllBytes(path)))
                .andExpect(status().is(204));
    }

    @Test
    public void shouldUploadXmlFile() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "prefs.xml",
                APPLICATION_XML_VALUE, getSystemResourceAsStream("sys.xml"));
        mvc.perform(fileUpload("/v1").file(file))
                .andExpect(status().is(204));
    }

}