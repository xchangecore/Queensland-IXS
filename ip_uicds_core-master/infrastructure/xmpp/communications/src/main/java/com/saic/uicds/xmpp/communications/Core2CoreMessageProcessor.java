package com.saic.uicds.xmpp.communications;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.springframework.integration.core.MessageChannel;

import com.saic.uicds.xmpp.extensions.core2coremessage.Core2CoreMessageIQFactory;

public class Core2CoreMessageProcessor {

    private Logger log = Logger.getLogger(this.getClass());

    private CoreConnection coreConnection;

    public CoreConnection getCoreConnection() {
        return coreConnection;
    }

    public void setCoreConnection(CoreConnection coreConnection) {
        this.coreConnection = coreConnection;
    }

    private MessageChannel core2CoreMessageNotificationChannel;

    public MessageChannel getCore2CoreMessageNotificationChannel() {
        return core2CoreMessageNotificationChannel;
    }

    public void setCore2CoreMessageNotificationChannel(
            MessageChannel core2CoreMessageNotificationChannel) {
        this.core2CoreMessageNotificationChannel = core2CoreMessageNotificationChannel;
    }

    @PostConstruct
    public void initialize() {
        log.debug("====#> Core2CoreMessageProcessor:initialize() ");
        assert (coreConnection != null);

        IQNamespacePacketFilter filter = new IQNamespacePacketFilter(
                Core2CoreMessageIQFactory.namespace);
        PacketTypeFilter iqFilter = new PacketTypeFilter(IQ.class);
        AndFilter andFilter = new AndFilter(iqFilter, filter);

        log.debug("====#> Core2CoreMessageProcessor:initialize() - adding Core2CoreMessageIQListener");
        coreConnection.addPacketListener(new Core2CoreMessageIQListener(this), andFilter);
    }
}
