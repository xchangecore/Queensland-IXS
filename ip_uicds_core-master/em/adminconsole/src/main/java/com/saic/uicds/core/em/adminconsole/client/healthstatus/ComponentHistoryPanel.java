package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * ComponentHistoryPanel
 * 
 * Panel inside the HistoryDialogBox, contains Label with timestamp, status and a TextArea for log
 * message
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class ComponentHistoryPanel extends ScrollPanel implements HealthStatusConstants {

    private VerticalPanel historyPanel = new VerticalPanel();

    public ComponentHistoryPanel() {
        historyPanel.setSize("300px", "280px");
        setSize("325px", "300px");
        add(historyPanel);
    }

    public void addMessageToLogHistory(String timeStamp, String status, String message) {
        TextArea messageArea = new TextArea();
        messageArea.setTitle("LogMessage");
        messageArea.setSize("300px", "200px");
        messageArea.setStyleName("applyBorder");
        messageArea.setText(message);
        messageArea.setStyleName(TEXT_AREA_BORDER);
        DisclosurePanel discPanel = new DisclosurePanel(timeStamp + " [" + status + "]");
        discPanel.setAnimationEnabled(true);
        discPanel.setContent(messageArea);
        historyPanel.add(discPanel);
    }
}
