package org.alfresco.service.transform;

public class Params {
    private final String name;
    private final boolean required;

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    Params(String name, boolean required)
    {
        this.name = name;
        this.required = required;
    }
}
