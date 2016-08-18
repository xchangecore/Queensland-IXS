package com.saic.uicds.core.em.exceptions;

import com.saic.uicds.core.infrastructure.exceptions.UICDSException;

@SuppressWarnings("serial")
public class DetailedCFSMessageException extends UICDSException {
    private String elementName;

    public DetailedCFSMessageException(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    @Override
    public String getMessage() {
        String message = " Missing element name " + elementName;
        return message;
    }

}
