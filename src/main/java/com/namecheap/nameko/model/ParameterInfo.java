package com.namecheap.nameko.model;

public class ParameterInfo {
    public final String name;
    public final String type;
    public final String defaultValue;

    public ParameterInfo(String name, String type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}