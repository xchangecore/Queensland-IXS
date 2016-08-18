package com.saic.uicds.xmpp.apps;

import gnu.getopt.Getopt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.xmpp.communications.InterestGroup;
import com.saic.uicds.xmpp.communications.InterestGroupManager;

public class SimpleCoreRoger {

    Logger logger = Logger.getLogger(CleanupXMPPNodes.class);

    private InterestGroupManager interestGroupManager;

    private SimpleCoreMessageResponder simpleCoreMessageResponder;

    @SuppressWarnings("unused")
	private SimpleCoreMessageReceiver simpleCoreMessageReceiver;

    @SuppressWarnings("unused")
	private String interestGroupName;
    private String interestGroupID = "INCIDENT_1";
    private String interestGroupWPID = "IncidentWP-1";
    private String interestGroupType = "Incident";
    private String workProduct = "<document>text</document>";
    private String remoteCore = "hauar-m4300";

    public InterestGroupManager getInterestGroupManager() {
        return interestGroupManager;
    }

    public void setInterestGroupManager(InterestGroupManager interestGroupManager) {
        this.interestGroupManager = interestGroupManager;
    }

    public SimpleCoreMessageResponder getSimpleCoreMessageResponder() {
        return simpleCoreMessageResponder;
    }

    public void setSimpleCoreMessageResponder(SimpleCoreMessageResponder simpleCoreMessageResponder) {
        this.simpleCoreMessageResponder = simpleCoreMessageResponder;
    }

    public void setSimpleCoreMessageReceiver(SimpleCoreMessageReceiver r) {
        simpleCoreMessageReceiver = r;
    }

    public void setInterestGroupID(String i) {
        interestGroupID = i;
    }

    public void setInterestGroupName(String i) {
        interestGroupName = i;
    }

    public void setInterestGroupType(String i) {
        interestGroupType = i;
    }

    public void setWorkProduct(String w) {
        workProduct = w;
    }

    public void setRemoteCore(String r) {
        remoteCore = r;
    }

    public void initialize(String[] args) {
    }

    private void receive() {
        boolean running = true;
        while (running) {
            Thread.yield();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                running = false;
            }
        }
        interestGroupManager.getCoreConnection().disconnect();
    }

    private void waitForRemoteCore() {
        int giveUp = 30;
        int attempt = 0;
        while (!interestGroupManager.getCoreConnection().isCoreOnline(remoteCore)) {
            Thread.yield();
            try {
                Thread.sleep(1000);
                if (attempt++ > giveUp) {
                    break;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void send() {
        InterestGroup interestGroup = new InterestGroup();
        interestGroup.interestGroupID = interestGroupID;
        interestGroup.interestGroupType = interestGroupType;
        interestGroup.interestGroupOwner = remoteCore;

        interestGroupManager.createInterestGroup(interestGroup);
        interestGroupManager.publishWorkProduct(interestGroupID, interestGroupWPID, "Incident",
                workProduct);

        waitForRemoteCore();

        List<String> workProductIDs = new ArrayList<String>();
        workProductIDs.add("Incident");
        String interestGroupInfo = "";
        interestGroupManager.shareInterestGroup(interestGroupID, remoteCore, interestGroupInfo,
                workProductIDs);
    }

    public static void main(String[] args) {

        boolean sender = false;

        String sopts = "rs";
        String programName = System.getProperty("program.name", "SimpleCoreRoger");
        Getopt getopt = new Getopt(programName, args, sopts);
        int code;
        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case 'r':
                sender = false;
                break;
            case 's':
                sender = true;
                break;
            }
        }

        BasicConfigurator.configure();

        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/simpleCoreRoger-applicationContext.xml" });

        SimpleCoreRoger core = (SimpleCoreRoger) context.getBean("simpleCoreRoger");

        core.initialize(args);

        if (sender) {
            core.send();
        } else {
            core.receive();
        }
    }
}
