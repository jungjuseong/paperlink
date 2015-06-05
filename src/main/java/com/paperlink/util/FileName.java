package com.paperlink.util;

public class FileName {
    //static String fulllPath;

    public FileName(){}

    static public String extension(String fullPath) {
        int dot = fullPath.lastIndexOf('.');
        return fullPath.substring(dot + 1);
    }

    static public String filename(String fullPath) { // gets filename without extension
        int dot = fullPath.lastIndexOf('.');
        int sep = fullPath.lastIndexOf('/');
        return fullPath.substring(sep + 1, dot);
    }

    static public String path(String fullPath) {
        int sep = fullPath.lastIndexOf('/');
        return fullPath.substring(0, sep);
    }
    static public String modify(String fullPath, String trailor, String extension) {
        return FileName.path(fullPath) + "/" + FileName.filename(fullPath) + trailor + "." + extension;
    }
}