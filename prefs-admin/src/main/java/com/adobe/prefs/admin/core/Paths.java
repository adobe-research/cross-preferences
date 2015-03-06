package com.adobe.prefs.admin.core;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public final class Paths {
    private Paths() {}

    private static final Joiner JOINER = Joiner.on('/');
    private static final Splitter SPLITTER = Splitter.on('/');

    public static String path(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            boolean leftEndsWithSlash = sb.length() != 0 && sb.charAt(sb.length() - 1) == '/';
            boolean rightStartsWithSlash = segment != null && !segment.isEmpty() && segment.charAt(0) == '/';
            if (!leftEndsWithSlash && !rightStartsWithSlash) {
                sb.append('/');
            }
            if (leftEndsWithSlash && rightStartsWithSlash) {
                segment = segment.substring(1);
            }
            if (segment != null) {
                sb.append(segment);
            }
        }
        return JOINER.join(Iterables.transform(SPLITTER.split(sb), UrlIO.ENCODER));
    }

    public static String parent(String path) {
        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash + 1);
        } else {
            return null;
        }
    }

}
