package com.saic.uicds.core.em.adminconsole.client.stropheXmpp;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Connection
 * 
 * JSNI to use strophe and implement xmpp
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class Connection {

    JavaScriptObject connection;
    String rid = null;
    String sid = null;

    public Connection(String boshService) {
        this.connection = connection(boshService);
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getRidValue() {
        return rid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSidValue() {
        return sid;
    }

    public native JavaScriptObject connection(String boshService) /*-{
                                                                   var connection = new $wnd.Strophe.Connection(boshService);
                                                                   return connection;
                                                                   }-*/;

    public native void doconnect(String jid, String password, ConnectionStatus c) /*-{
                                                                                   var c = c.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.ConnectionStatus::wrapper()();
                                                                                   var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                                                                   connection.rawInput = function log(msg){
                                                                                   $wnd.Strophe.info(msg);
//                                                                                    $wnd.console.log("[RawInput]: "+msg);
                                                                                   };
                                                                                   connection.rawOutput = function log(msg){
//                                                                                   $wnd.console.log("[RawOutput]: "+msg);
                                                                                   };
                                                                                   connection.connect(jid, password, c);
//                                                                                    $wnd.onbeforeunload = function(evt) {
//                                                                                      connection.pause();
//                                                                                  };

//                                                                                  $wnd.onunload = function(evt) {
//                                                                                      connection.pause();
//                                                                                  };
                                                                                   }-*/;

    public native void addMessageHandler(String ns, String name, String type, String id,
            Handler<Element> handler) /*-{
                                      var h = handler.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Handler::wrapper()();
                                      var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                      connection.addHandler(h, null, type, id);
                                      }-*/;

    public native void doSendMessage(String text) /*-{
                                                  var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                                  var to=connection.jid;
                                                  var msg = $wnd.$msg({to: to})
                                                  .c('body').t(text);
                                                  connection.send(msg);
                                                  }-*/;

    public native void doSendMessage(String text, String to) /*-{
                                                             var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                                             var msg = $wnd.$msg({to: to})
                                                             .c('body').t(text);
                                                             connection.send(msg);
                                                             }-*/;

    public native void goOnline() /*-{
                                   var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                   connection.send($wnd.$pres());
                                   }-*/;

    public native void goOffline() /*-{
                                    var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                    connection.send($wnd.$pres({type: "unavailable"}));
                                    }-*/;

    public native String getCoreJid()/*-{
                                     var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                     return connection.jid;
                                     }-*/;

    public native void doDisconnect() /*-{
                                      var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                      //                                      connection.sync = true;
                                      //                                      connection.flush();
                                      connection.disconnect();
                                      }-*/;

    public native void doReset() /*-{
                                      var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                      connection.reset();
                                      }-*/;

    public native void getSid() /*-{
                                var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                var sid=connection.sid;
                                var s=""+sid.valueOf();
                                this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::setSid(Ljava/lang/String;)(s);
                                }-*/;

    public native void getRid() /*-{
                                var connection = this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::connection;
                                var rid=connection.rid;
                                var r=""+rid.valueOf();
                                this.@com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection::setRid(Ljava/lang/String;)(r);
                                }-*/;

}
