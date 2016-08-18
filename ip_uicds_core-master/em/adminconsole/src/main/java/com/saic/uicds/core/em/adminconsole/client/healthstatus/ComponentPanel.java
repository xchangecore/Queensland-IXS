package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanelImages;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * ComponentPanel
 * 
 * Panel contains ComponentLabel, ComponentStatus-ColorButton, and ComponentStatusPanel
 * Handles Messages, Builds Log Message History
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
@SuppressWarnings("deprecation")
public class ComponentPanel extends VerticalPanel implements HealthStatusConstants {

    private Label componentLabel;
    
    private Label statusLabel;

    private String componentName;

    private Button componentButton;

    private String currentLogMessage = "Component";

    HorizontalPanel componentPanel = new HorizontalPanel();
    ComponentStatusPanel statusPanel;
    DisclosurePanelImages images = (DisclosurePanelImages) GWT.create(DisclosurePanelImages.class);
    Image openImage;
    Image closeImage;

    public void setCurrentLogMessage(String currentLogMessage) {
        this.currentLogMessage = currentLogMessage;
    }

    private List<String> errorHistory = new ArrayList<String>();

    public void appendLogHistory(String componentId, String status, String timeStamp,
            String logMessage, String message) {
        this.errorHistory.add(message);
        statusPanel.historyPanel.addMessageToLogHistory(timeStamp, status, logMessage);
    }

	public ComponentPanel(String component) {
    	this.componentName=component;
        openImage = images.disclosurePanelOpen().createImage();
        openImage.setAltText("Expand");
        openImage.setVisible(false);
        closeImage = images.disclosurePanelClosed().createImage();
        closeImage.setAltText("Collapse");
        openImage.addClickHandler(componentClickHandler);
        closeImage.addClickHandler(componentClickHandler);
        
        HorizontalPanel imagePanel = new HorizontalPanel();
        imagePanel.add(openImage);
        imagePanel.add(closeImage);
        
        componentLabel = new Label(component);
        componentLabel.addClickHandler(componentClickHandler);
        statusLabel = new Label("Status: "+NORMAL);
        
        componentButton = new Button();
        componentButton.setSize("13px", "13px");
        componentButton.setFocus(true);
        componentButton.setEnabled(true);
        componentButton.setStyleName(GREEN_BUTTON_STYLE);
        
        componentButton.addFocusListener(new FocusListener() {
			@Override
			public void onLostFocus(Widget arg0) {
				componentButton.removeStyleName("highlightBorder");
			}
			@Override
			public void onFocus(Widget arg0) {
				componentButton.addStyleName("highlightBorder");
			}
		});
        componentButton.addKeyboardListener(new KeyboardListener() {
			@Override
			public void onKeyUp(Widget arg0, char arg1, int arg2) {
			}
			@Override
			public void onKeyPress(Widget arg0, char arg1, int arg2) {
				if(arg1==KeyboardListener.KEY_ENTER)
					toggleComponent();
			}
			@Override
			public void onKeyDown(Widget arg0, char arg1, int arg2) {
			}
        });
        componentButton.getElement().setAttribute("aria-label", component+" Component: Status is "+ getStatus(GREEN_BUTTON_STYLE) );
        HorizontalPanel componentHPanel = new HorizontalPanel();
        componentHPanel.setSize("400px", "20px");
        componentHPanel.add(componentLabel);
        componentHPanel.setCellHorizontalAlignment(componentLabel, HorizontalPanel.ALIGN_LEFT);
        componentHPanel.setCellVerticalAlignment(componentLabel, VerticalPanel.ALIGN_MIDDLE);
        componentHPanel.add(statusLabel);
        componentHPanel.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_RIGHT);
        componentHPanel.add(componentButton);
        componentHPanel.setCellHorizontalAlignment(componentButton, HorizontalPanel.ALIGN_RIGHT);
        componentHPanel.setCellVerticalAlignment(componentButton, VerticalPanel.ALIGN_MIDDLE);

        componentPanel.setSpacing(5);
        componentPanel.setWidth("450px");
        componentPanel.setStyleName(ORIGINAL_BORDER_STYLE);
        componentPanel.add(imagePanel);
        componentPanel.add(componentHPanel);
        componentPanel.setCellHorizontalAlignment(imagePanel, HorizontalPanel.ALIGN_CENTER);
        componentPanel.setCellVerticalAlignment(imagePanel, VerticalPanel.ALIGN_MIDDLE);
        componentPanel.setCellHorizontalAlignment(componentHPanel, HorizontalPanel.ALIGN_CENTER);
        componentPanel.setCellVerticalAlignment(componentHPanel, VerticalPanel.ALIGN_MIDDLE);

        statusPanel = new ComponentStatusPanel();
        DOM.setAttribute(statusPanel.historyLinkLabel.getElement(), "tabIndex", "-1");
        statusPanel.setVisible(false);

        add(componentPanel);
        add(statusPanel);
        
        
    }

    private String getStatus(String styleName) {
    	if(styleName.equals(ORANGE_BUTTON_STYLE))
    		return "WARN";
    	else if(styleName.equals(RED_BUTTON_STYLE))
    		return "ERROR";
		return "NORMAL";
	}

	public ClickHandler componentClickHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg0) {
            toggleComponent();
        }
    };

    public void toggleComponent() {
        toggleBorder();
        toggleVisibility();
    }

    private void toggleBorder() {
        String labelStyle = componentLabel.getStyleName();
        if (labelStyle.equals(SELECTED_LABEL_STYLE)) {
            componentLabel.removeStyleName(labelStyle);
        } else {
            componentLabel.setStyleName(SELECTED_LABEL_STYLE);
        }
        String panelStyle = componentPanel.getStyleName();
        if (panelStyle.equals(SELECTED_BORDER_STYLE)) {
            componentPanel.setStyleName(ORIGINAL_BORDER_STYLE);
            statusPanel.historyLinkLabel.getElement().setAttribute("tabIndex", "-1");
        } else if (panelStyle.equals(ORIGINAL_BORDER_STYLE)) {
            componentPanel.setStyleName(SELECTED_BORDER_STYLE);
            statusPanel.historyLinkLabel.getElement().setAttribute("tabIndex", "0");
        }
    }

    public void toggleVisibility() {
        boolean visibility = statusPanel.isVisible();
        if (visibility) {
            statusPanel.setVisible(false);
            openImage.setVisible(false);
            closeImage.setVisible(true);
        } else {
            statusPanel.setVisible(true);
            openImage.setVisible(true);
            closeImage.setVisible(false);
            statusPanel.statusArea.setText(currentLogMessage);
        }
    }

    public void resetComponentStyles() {
        VerticalPanel parent = (VerticalPanel) getParent();
        for (int i = 1; i < parent.getWidgetCount() - 1; i++) {
            ComponentPanel component = (ComponentPanel) parent.getWidget(i);
            String label = component.componentLabel.getStyleName();
            if (label.equals(SELECTED_LABEL_STYLE)) {
                component.componentLabel.removeStyleName(label);
            }
            String panelStyle = component.getStyleName();
            if (panelStyle.equals(SELECTED_BORDER_STYLE)) {
                component.setStyleName(ORIGINAL_BORDER_STYLE);
            }
        }
    }

    public void setStatusAreaText(String message, String applyColor) {
        statusPanel.statusArea.setText(message);
        if (applyColor != null) {
            statusPanel.statusButton.setStyleName(applyColor);
            statusPanel.issueLabel.setText("1 issue(s)");
            String status="NORMAL";
            if(applyColor.equals(RED_BUTTON_STYLE))
            	status="ERROR";
            else if(applyColor.equals(ORANGE_BUTTON_STYLE))
            	status="WARN";
            statusPanel.currentLabel.setText(CURRENT_STATUS+" "+status);
        }
        setCurrentLogMessage(message);
    }

    /**
     * Based on the status in the message apply styles to label,button
     * Add message to history
     */
    public void handleMessage(String componentId, String status, String timeStamp,
            String logMessage, String message) {
        String applyColor = componentButton.getStyleName();
        String label="Status: "+NORMAL;
        if (status.contains("ERROR")) {
            applyColor = RED_BUTTON_STYLE;
            label="Status: "+ERROR;
        } else if (status.contains("WARN")) {
            applyColor = ORANGE_BUTTON_STYLE;
            label="Status: "+WARN;
        } else if (status.contains("NORMAL")) {
            applyColor = GREEN_BUTTON_STYLE;
        } else if (status.contains("TIMEOUT")) {
            if (applyColor.equals(RED_BUTTON_STYLE)) {
                applyColor = ORANGE_BUTTON_STYLE;
                label="Status: "+WARN;
            } else {
                applyColor = GREEN_BUTTON_STYLE;
            }
        }
        componentButton.getElement().setAttribute("aria-label", componentName+" Component: Status is "+ getStatus(applyColor) );
        componentButton.setStyleName(applyColor);
        statusLabel.setText(label);
        setStatusAreaText(message, applyColor);

//        appendLogHistory(componentId, status, timeStamp, logMessage, message);
    }

}
