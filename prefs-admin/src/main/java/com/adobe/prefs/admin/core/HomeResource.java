package com.adobe.prefs.admin.core;

import org.springframework.hateoas.ResourceSupport;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resource")
public class HomeResource extends ResourceSupport {
    final String prefsType;

    HomeResource() {    // JAXB
        this(null);
    }

    public HomeResource(final String prefsType) {
        this.prefsType = prefsType;
    }

    @XmlElement
    public String getContent() {
        return prefsType;
    }
}
