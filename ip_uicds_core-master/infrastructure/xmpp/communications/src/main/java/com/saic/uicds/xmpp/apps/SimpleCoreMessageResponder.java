package com.saic.uicds.xmpp.apps;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageHandler;

public class SimpleCoreMessageResponder {

    // key: MessageChannel.getName()
    private HashMap<String, EventDrivenConsumer> consumerMap = new HashMap<String, EventDrivenConsumer>();

    private Logger logger = Logger.getLogger(this.getClass());

//    private class JoinMessageHandler implements MessageHandler {
//
//        @Override
//        public void handleMessage(Message<?> arg0) {
//            logger.info(" =====> got interestGroupStateNotificationChannel <==== ");
//        }
//
//    }

    protected void startChannel(MessageChannel channel, MessageHandler handler) {
        EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) channel,
                handler);
        consumer.start();
        if (consumer != null) {
            logger.info(" event consumer created on channel " + channel.getName());
            consumerMap.put(channel.getName(), consumer);
        } else {
            logger.error("event consumer not created for channel " + channel.getName());
        }
    }

}
