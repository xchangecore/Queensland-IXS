package com.saic.uicds.xmpp.apps;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.xmpp.communications.InterestGroup;
import com.saic.uicds.xmpp.communications.InterestGroupManager;
import com.saic.uicds.xmpp.communications.InterestManager;

public class InterestGroupDumper {

    InterestManager interestManager;
    InterestGroupManager interestGroupManager;

    public void setInterestGroupManager(InterestGroupManager interestGroupManager) {
        this.interestGroupManager = interestGroupManager;
    }

    public void setInterestManager(InterestManager m) {
        interestManager = m;
    }

    public InterestGroupDumper() {
        // initialize();
    }

    public void dump() {

        Map<String, InterestGroup> interestGroupMap = interestGroupManager.getInterestGroupList();

        String xml = "<interestGroups>";
        Set<String> keys = interestGroupMap.keySet();
        for (String key : keys) {
            System.out.println(interestGroupMap.get(key));
            // xml += interestGroup.dumpXML();
        }
        xml += "</interestGroups>";

        XmlObject obj = null;
        try {
            obj = XmlObject.Factory.parse(xml);
        } catch (XmlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        XmlOptions xo = new XmlOptions();
        xo.setSavePrettyPrint();
        // System.out.println(obj.xmlText(xo));
        File file = new File("interestGroups.xml");
        try {
            obj.save(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cleanup() {
        interestManager.unsubscribeAll(interestGroupManager.getCoreConnection().getPubSubSvc());
    }

    // public List<InterestGroup> getInterestGroups() {
    // updateInterestGroupList();
    // return interestGroups;
    // }
    //	
    // public InterestGroup getInterestGroup(String uuid) {
    // InterestGroup i = null;
    // for (InterestGroup interestGroup : interestGroups) {
    // if (interestGroup.getUuid().equals(uuid)) {
    // i = interestGroup;
    // break;
    // }
    // }
    // return i;
    // }

    /**
     * @param args
     */
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/applicationContext.xml" });

        // String sopts = "p:r:";
        // String programName = System.getProperty("program.name", "InterestGroupDumper");
        // Getopt getopt = new Getopt(programName, args, sopts);
        // int code;
        // String propsFile = null;
        // String rosterPropsFile = null;
        // while ((code = getopt.getopt()) != -1) {
        // switch (code)
        // {
        // case 'p':
        // propsFile = getopt.getOptarg();
        // System.out.println("Props file: "+propsFile);
        // break;
        // case 'r':
        // rosterPropsFile = getopt.getOptarg();
        // System.out.println("Roster file: "+rosterPropsFile);
        // break;
        // }
        // }
        //		
        // if (propsFile == null || rosterPropsFile == null) {
        // System.err.println("Usage: -p <XMPP config props file> -r <Roster props file>");
        // }
        // else {
        InterestGroupDumper dumper = (InterestGroupDumper) context.getBean("interestGroupDumper");
        dumper.dump();
        dumper.cleanup();
        System.out.println("DONE");
        // }

    }

}
