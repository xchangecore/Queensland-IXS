package com.saic.uicds.xmpp.apps;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.xmpp.apps.CoreXMPPUtils.XMPPNode;
import com.saic.uicds.xmpp.communications.CoreConnection;

public class DumpXMPPNodes {

    Logger logger = Logger.getLogger(DumpXMPPNodes.class);

    CoreConnection coreConnection;

    public void setCoreConnection(CoreConnection c) {
        coreConnection = c;
    }

    CoreXMPPUtils coreXMPPUtils;

    public void setCoreXMPPUtils(CoreXMPPUtils c) {
        coreXMPPUtils = c;
    }

    /**
     * @param args
     */
    private void dumpNodes() {
        // Set<CoreXMPPUtils.XMPPNode> nodeSet = coreXMPPUtils.getNodeSet();
        Set<XMPPNode> nodes = coreXMPPUtils.getXMPPNodesRecusivly(coreConnection
                .getInterestGroupRoot());
        if (!nodes.isEmpty()) {
            Iterator<XMPPNode> it = nodes.iterator();
            System.out.println(it.next());
        }
    }

    public static void main(String[] args) {

        BasicConfigurator.configure();

        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/applicationContext.xml" });

        DumpXMPPNodes dumpXMPPNodes = (DumpXMPPNodes) context.getBean("dumpXMPPNodes");

        dumpXMPPNodes.dumpNodes();
    }

}
