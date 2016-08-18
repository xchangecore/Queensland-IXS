package com.saic.uicds.xmpp.extensions.core2coremessage;

import org.jivesoftware.smack.packet.IQ;

import com.saic.uicds.xmpp.extensions.util.ArbitraryIQ;

public class Core2CoreMessageIQFactory {
    public static final String elementName = "core2coremessage";
    public static final String namespace = "http://uicds.saic.com/xmpp/extensions/core2coremessage";

    public static IQ createCore2CoreMessage(String message, String msgType, String toCoreJID,
            String toCoreJIDWirhResource) {
        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(toCoreJIDWirhResource);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        StringBuffer params = new StringBuffer();
        params.append(" toCoreJID='" + toCoreJID + "'");
        params.append(" msgType='" + msgType + "'");

        sb.append("<sendMessage");
        sb.append(params);
        sb.append(">");

        sb.append(message);
        sb.append("</sendMessage>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());
        return iq;
    }
}
