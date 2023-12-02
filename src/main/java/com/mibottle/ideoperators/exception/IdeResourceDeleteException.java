package com.mibottle.ideoperators.exception;

import com.mibottle.ideoperators.customresource.IdeConfig;

public class IdeResourceDeleteException extends Exception {
    private final IdeConfig resource;
    private final String message;

    public IdeResourceDeleteException(IdeConfig resource, String message, Throwable cause) {
        super(message, cause);
        this.resource = resource;
        this.message = message;
    }

    public IdeConfig getResource() {
        return resource;
    }

    @Override
    public String getMessage() {
        return message + "; Resource Info: " + resource.toString();
    }
}

