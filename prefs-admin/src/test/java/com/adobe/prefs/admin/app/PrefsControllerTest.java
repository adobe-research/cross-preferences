package com.adobe.prefs.admin.app;

import com.adobe.prefs.admin.infra.HtmlMessageConverter;
import org.hamcrest.core.StringStartsWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2CollectionHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.nio.file.Files.readAllBytes;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PrefsControllerTest {

    final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PrefsController()
    ).setMessageConverters(
            new Jaxb2RootElementHttpMessageConverter(),
            new Jaxb2CollectionHttpMessageConverter<>(),
            new MappingJackson2HttpMessageConverter(),
            new HtmlMessageConverter()
    ).build();

    final String child = "child/";
    final String key = "key";

    @Test(dataProvider = "prefs")
    public void shouldListRootContentsAsJson(String p) throws Exception {
        mvc.perform(get(p).accept(APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("links").exists())
                .andExpect(jsonPath("content").isArray());
    }

    @Test(dataProvider = "prefs")
    public void shouldCreateChild(String p) throws Exception {
        mvc.perform(put(p + child))
                .andExpect(status().is(303))
                .andExpect(header().string("Location", p));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldCreateChild")
    public void shouldListChild(String p) throws Exception {
        mvc.perform(get(p).accept(APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("links[0].href").value(p))
                .andExpect(jsonPath("links[2].href").value(p + child));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldCreateChild")
    public void shouldReadChildAsXml(String p) throws Exception {
        final String path = p + child;
        mvc.perform(get(path).contentType(APPLICATION_XML))
                .andExpect(status().is(200))
                .andExpect(xpath("/resource/content").exists())
                .andExpect(xpath("/resource/link[@rel='self']/@href").string(path));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldCreateChild")
    public void shouldCreateKey(String p) throws Exception {
        final String path = p + child + key;
        mvc.perform(put(path).contentType(APPLICATION_FORM_URLENCODED).param("value", "val"))
                .andExpect(status().is(303))
                .andExpect(header().string("Location", p + child));
    }

    @Test(dataProvider = "prefs")
    public void shouldNotAllowEmptyValues(String p) throws  Exception {
        final String path = p + child + key;
        mvc.perform(put(path).contentType(APPLICATION_FORM_URLENCODED).param("value", ""))
                .andExpect(status().is(400));
        mvc.perform(put(path).contentType(APPLICATION_FORM_URLENCODED))
                .andExpect(status().is(400));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldCreateKey")
    public void shouldReadKeyAsJson(String p) throws Exception {
        final String path = p + child + key;
        mvc.perform(get(path).accept(APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(jsonPath("key").value(key))
                .andExpect(jsonPath("value").value("val"));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldReadKeyAsJson")
    public void shouldRemoveKey(String p) throws Exception {
        final String path = p + child + key;
        mvc.perform(delete(path))
                .andExpect(status().is(303));
        mvc.perform(get(path))
                .andExpect(status().is(404));
    }

    @Test(dataProvider = "prefs", dependsOnMethods = "shouldRemoveKey")
    public void shouldRemoveChild(String p) throws Exception {
        final String path = p + child;
        mvc.perform(delete(path))
                .andExpect(status().is(303));
        mvc.perform(get(path))
                .andExpect(status().is(404));
    }

    @Test(dataProvider = "prefs")
    public void shouldExportInlineContents(String p) throws Exception {
        mvc.perform(get(p).param("export", ""))
                .andExpect(status().is(200))
                .andExpect(content().contentType(APPLICATION_XML))
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test(dataProvider = "prefs")
    public void shouldExportContentsAsFile(String p) throws Exception {
        mvc.perform(get(p).param("export", "file"))
                .andExpect(status().is(200))
                .andExpect(content().contentType(APPLICATION_XML))
                .andExpect(header().string("Content-Disposition", new StringStartsWith("attachment;")));
    }


    @Test
    public void shouldRedirectToSystemRoot() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is(302))
                .andExpect(header().string("Location", "/sys/"));
    }

    @Test
    public void shouldUploadInlineXml() throws Exception {
        final Path path = Paths.get(ClassLoader.getSystemResource("usr.xml").toURI());
        mvc.perform(post("/").contentType(APPLICATION_XML).content(readAllBytes(path)))
                .andExpect(status().is(303))
                .andExpect(header().string("Location", "/"));
    }

    @Test
    public void shouldUploadXmlFile() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "prefs.xml",
                APPLICATION_XML_VALUE, getSystemResourceAsStream("sys.xml"));
        mvc.perform(fileUpload("/").file(file))
                .andExpect(status().is(303))
                .andExpect(header().string("Location", "/"));
    }


    @DataProvider
    public Object[][] prefs() {
        return new String[][] {
                { "/usr/test/" },
                { "/sys/test/" }
        };
    }

}
