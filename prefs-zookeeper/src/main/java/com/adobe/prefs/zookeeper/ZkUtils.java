package com.adobe.prefs.zookeeper;

import java.io.UnsupportedEncodingException;

import static com.google.common.base.Strings.isNullOrEmpty;

final class ZkUtils {

    private ZkUtils() {}

    static final String UTF8 = "UTF-8";
    static final char PATH_SEP = '/';

    static byte[] bytes(String s) {
        if (s == null) {
            return null;
        }
        try {
            return s.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("WTF-8", e);
        }
    }

    static String string(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("WTF-8", e);
        }
    }

    static String basename(String path) {
        return path.substring(path.lastIndexOf(PATH_SEP) + 1);
    }

    static String namespace(String path) {
        if (isNullOrEmpty(path)) {
            return null;
        }
        while (!path.isEmpty() && path.charAt(0) == PATH_SEP) {
            path = path.substring(1);
        }
        while (!path.isEmpty() && path.charAt(path.length() - 1) == PATH_SEP) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? null : path;
    }
}
