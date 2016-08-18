package com.saic.uicds.core.em.adminconsole.client.stropheXmpp;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * ConnectionStatus
 * 
 * Handler for connection status
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public abstract class ConnectionStatus {
    public abstract void statusChanged(Status status, String reason);

    public enum Status {
        ERROR, CONNECTING, CONNFAIL, AUTHENTICATING, AUTHFAIL, CONNECTED, DISCONNECTED, DISCONNECTING;
    }

    private void statusChanged(int code, String reason) {
        Status status = null;
        for (Status s : Status.values())
            if (s.ordinal() == code)
                status = s;
        statusChanged(status, reason);
    }

    native JavaScriptObject wrapper() /*-{
                                      var callback = this;
                                      return function(code, reason) {
                                      callback.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.ConnectionStatus::statusChanged(ILjava/lang/String;)(code, reason);
                                      }
                                      }-*/;
}