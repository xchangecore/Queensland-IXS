package com.saic.uicds.core.em.adminconsole.client.stropheXmpp;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Handler
 * 
 * Handler for xmpp response
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public abstract class Handler<T> {

    public abstract boolean handle(Object element);

    native JavaScriptObject wrapper() /*-{
                                      var handler = this;
                                      return function(element) {
                                      return handler.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Handler::handle(Ljava/lang/Object;)(element);
                                      }
                                      }-*/;

}