package com.adobe.prefs.admin.core;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.prefs.Preferences;

@XmlRootElement(name = "preference")
public class PrefResource extends ResourceSupport {

    private final Preferences parent;
    private final String key;
    private final String value;

    PrefResource() {   // FU, JAXB!
        throw new IllegalStateException();
    }

    public PrefResource(String realm, Preferences parent, String key, boolean withParentLink) {
        this.parent = parent;
        this.key = key;
        this.value = parent.get(key, null);
        add(new Link(Paths.path(realm, parent.absolutePath(), key)));
        if (withParentLink) {
            add(new Link(Paths.path(realm, parent.absolutePath(), null), "parent"));
        }
    }

    @XmlElement
    public String getKey() {
        return key;
    }

    @XmlElement
    public String getValue() {
        return value != null ? value : "";
    }

}
