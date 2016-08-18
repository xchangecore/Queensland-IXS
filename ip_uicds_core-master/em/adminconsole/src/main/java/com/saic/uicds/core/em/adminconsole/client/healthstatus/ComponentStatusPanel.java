package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * ComponentStatusPanel
 * 
 * Panel with CurrentStatus, Log History, and TextAre for log Message 
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class ComponentStatusPanel extends VerticalPanel implements HealthStatusConstants{

    public Label currentLabel=new Label(CURRENT_STATUS+" "+NORMAL);
    public Button statusButton;
    public Label issueLabel=new Label("0 issue(s)");
    public Button historyLinkLabel=new Button(VIEW_HISTORY);
    public TextArea statusArea=new TextArea();
    CustomDialogBox historyDialogBox;
    public static final String GREEN_BUTTON_STYLE = "applyGreenColor";
    VerticalPanel panel=new VerticalPanel();
    ComponentHistoryPanel historyPanel;
    HorizontalPanel statusPanel=new HorizontalPanel();
    
    public ComponentStatusPanel(){
        
        statusPanel.setWidth("450px");
        statusPanel.setSpacing(3);

        currentLabel.setWidth("70%");
        statusButton=new Button();
        statusButton.setFocus(true);
        statusButton.setEnabled(false);
        statusButton.setSize("13px", "13px");
        statusButton.setStyleName(GREEN_BUTTON_STYLE);
        statusButton.getElement().setAttribute("aria-label", "Component status and it is green");
        historyLinkLabel.addClickHandler(historyHandler);
        
        statusArea.setSize("440px", "125px");
        statusArea.setTitle("ComponentStatus");

        statusPanel.add(currentLabel);
        statusPanel.setCellHorizontalAlignment(currentLabel, HorizontalPanel.ALIGN_LEFT);
        statusPanel.setCellVerticalAlignment(currentLabel, VerticalPanel.ALIGN_MIDDLE);
        
        statusPanel.add(statusButton);
        statusPanel.setCellHorizontalAlignment(statusButton, HorizontalPanel.ALIGN_LEFT);
        statusPanel.setCellVerticalAlignment(statusButton, VerticalPanel.ALIGN_MIDDLE);
        statusPanel.add(issueLabel);
        statusPanel.setCellHorizontalAlignment(issueLabel, HorizontalPanel.ALIGN_LEFT);
        statusPanel.setCellVerticalAlignment(issueLabel, VerticalPanel.ALIGN_MIDDLE);
        statusPanel.add(historyLinkLabel);
        statusPanel.setCellHorizontalAlignment(historyLinkLabel, HorizontalPanel.ALIGN_RIGHT);
        statusPanel.setCellVerticalAlignment(historyLinkLabel, VerticalPanel.ALIGN_MIDDLE);

        add(statusPanel);
        VerticalPanel statusAreaDec=new VerticalPanel();
        statusAreaDec.add(statusArea);
        statusAreaDec.setStyleName("textAreaBorder");
        add(statusAreaDec);

        historyPanel=new ComponentHistoryPanel();
        panel.add(historyPanel);
    }
    
    ClickHandler historyHandler=new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg0) {
            historyDialogBox=new CustomDialogBox(LOG_HISTORY,true);
            historyDialogBox.setWidget(panel);
            historyDialogBox.center();
            historyDialogBox.showDialog();
            historyDialogBox.getElement().setAttribute("tabIndex","0");  
        }
    };

}
