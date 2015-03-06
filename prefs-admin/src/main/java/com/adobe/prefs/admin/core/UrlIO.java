package com.adobe.prefs.admin.core;

import com.google.common.base.Function;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;

public enum UrlIO implements Function<String, String> {
    ENCODER {
        @Override public String apply(String input) {
            try {
                return input != null ? UriUtils.encodeQueryParam(input, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("WTF-8");
            }
        }
    },
    DECODER {
        @Override public String apply(String input) {
            try {
                return input != null ? UriUtils.decode(input, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("WTF-8");
            }
        }
    }
}
