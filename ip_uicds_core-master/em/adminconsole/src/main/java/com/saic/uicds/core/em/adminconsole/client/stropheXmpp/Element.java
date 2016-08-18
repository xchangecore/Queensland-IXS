package com.saic.uicds.core.em.adminconsole.client.stropheXmpp;

import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Handler;

/**
 * Element
 *
 * Response received from served in xml format. Parses the response 
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class Element extends com.google.gwt.user.client.Element {

	protected Element() {}

	public native static Element xmlElement(String name) /*-{
		return $wnd.Strophe.xmlElement(name);
	}-*/;
	
	public native static Element xmlTextNode(String text) /*-{
		return $wnd.Strophe.xmlTextNode(text);
	}-*/;

	public final native void forEachChild(String name, Handler<Element> handler) /*-{
		var h = handler.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Handler::wrapper()();
		$wnd.Strophe.forEachChild(this, name, h);
	}-*/;

	public final native boolean isTagEqual(String name) /*-{
		return $wnd.Strophe.isTagEqual(this, name);
	}-*/;

	public final native String getText() /*-{
		return $wnd.Strophe.getText(this);
	}-*/;

	public final native Element copy() /*-{
		return $wnd.Strophe.elementCopy(this);
	}-*/;

	public final native String serialize() /*-{
		return $wnd.Strophe.serialize(this);
	}-*/;

}
