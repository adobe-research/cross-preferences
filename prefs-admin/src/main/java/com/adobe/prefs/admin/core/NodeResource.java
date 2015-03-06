package com.adobe.prefs.admin.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@XmlRootElement(name = "resource")
@XmlSeeAlso(PrefResource.class)
public class NodeResource extends Resources<PrefResource> {
    private final Preferences prefs;

    NodeResource() {    // FU, JAXB!
        throw new IllegalStateException();
    }

    public NodeResource(final String realm, final Preferences prefs) {
        super(getPrefs(realm, prefs));
        this.prefs = prefs;
        add(new Link(Paths.path(realm, prefs.absolutePath(), null)));
        if (prefs.parent() != null) {
            add(new Link(Paths.path(realm, prefs.parent().absolutePath(), null), "parent"));
        }
        try {
            final String[] children = prefs.childrenNames();
            Arrays.sort(children);
            add(Iterables.transform(Arrays.asList(children), new Function<String, Link>() {
                @Override
                public Link apply(String childName) {
                    return new Link(Paths.path(realm, prefs.absolutePath(), childName, null), childName);
                }
            }));
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }

    }

    private static Iterable<PrefResource> getPrefs(final String realm, final Preferences prefs) {
        try {
            final String[] keys = prefs.keys();
            Arrays.sort(keys);
            return Iterables.transform(Arrays.asList(keys), new Function<String, PrefResource>() {
                @Override
                public PrefResource apply(String key) {
                    return new PrefResource(realm, prefs, key, false);
                }
            });
        } catch (BackingStoreException e) {
            throw Throwables.propagate(e);
        }
    }

    @JsonIgnore
    @XmlTransient
    public String getName() {
        return prefs.name().isEmpty() ? "/" : prefs.name();
    }

    @JsonIgnore
    @XmlTransient
    public String getParentName() {
        if (prefs.parent() != null) {
            return prefs.parent().name().isEmpty() ? "/" : prefs.parent().name();
        }
        return null;
    }

}
