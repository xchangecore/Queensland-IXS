package com.saic.uicds.core.infrastructure.messages;

public class Core2CoreMessage {

    String fromCore;
    String toCore;
    String messageType;
    String message;
    String body;
    String xhtml;

    public String getFromCore() {
        return fromCore;
    }

    public void setFromCore(String fromCore) {
        this.fromCore = fromCore;
    }

    public String getToCore() {
        return toCore;
    }

    public void setToCore(String toCore) {
        this.toCore = toCore;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getXhtml() {
		return xhtml;
	}

	public void setXhtml(String xhtml) {
		this.xhtml = xhtml;
	}

}
