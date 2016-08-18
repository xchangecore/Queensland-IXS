package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * corePanel
 * 
 * Panel with Corename Label and CoreStatus button
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class CorePanel extends HorizontalPanel implements HealthStatusConstants {

	private String coreId;

	public String getCoreId() {
		return coreId;
	}

	public void setCoreId(String coreId) {
		this.coreId = coreId;
	}

	private String coreStatus = "offline";
	private Label componentLabel;
	private Button componentButton = new Button();
	PopupPanel popup = new PopupPanel();
	Label popupLabel = new Label("");

	String enhancedLabel = "Not EAD Console\n (OR) EAD Console Not Loaded";

	public String getEnhancedLabel() {
		return enhancedLabel;
	}

	public String getEnhancedText() {
		String text = popupLabel.getText();
		if (text.equals("")) {
			return "Not EAD Console";
		}
		text = text.substring(text.lastIndexOf("-") + 2);
		return text;
	}

	public void setEnhancedLabel(String enhancedLabel) {
		this.enhancedLabel = enhancedLabel;
		String text = popupLabel.getText();
		text = text.substring(0, text.lastIndexOf("-") + 2);
		popupLabel.setText(text + enhancedLabel);

	}

	public CorePanel(String coreId, String coreStatus) {
		this.coreId = coreId;
		setCoreStatus(coreStatus);
		createPanel();
	}

	@SuppressWarnings("deprecation")
	private void createPanel() {
		setSpacing(5);
		setSize("320px", "25px");

		componentLabel = new Label(coreId+" [OFFLINE - No Status]");
		add(componentLabel);
		setCellHorizontalAlignment(componentLabel, HorizontalPanel.ALIGN_LEFT);
		setCellVerticalAlignment(componentLabel, VerticalPanel.ALIGN_MIDDLE);

		componentButton.setSize("13px", "13px");
		componentButton.setFocus(true);
		componentButton.setEnabled(true);
		componentButton.addMouseOverHandler(onMouseOver);
		componentButton.addMouseOutHandler(onMouseOut);
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

		popup.setWidget(popupLabel);

		add(componentButton);
		componentButton.getElement().setAttribute(
				"aria-label",
				"Shared core: " + coreId + popupLabel.getText());
		setCellHorizontalAlignment(getWidget(1), HorizontalPanel.ALIGN_RIGHT);
		setCellVerticalAlignment(getWidget(1), VerticalPanel.ALIGN_MIDDLE);
	}

	public void setCoreStatus(String coreStatus) {
		this.coreStatus = coreStatus;
		String shortStatus="[ONLINE - NO STATUS]";
		if (coreStatus.equalsIgnoreCase("online")) {
			String text = popupLabel.getText();
			if (!(text.contains("NORMAL") || text.contains("ERROR") || text
					.contains("WARN"))) {
				componentButton.setStyleName(GREEN_BUTTON_STYLE);
				popupLabel
						.setText("Status [No Status] - Not Enhanced Admin Console");
			}
		} else if (coreStatus.equalsIgnoreCase("offline")) {
			componentButton.setStyleName(GREY_BUTTON_STYLE);
			popupLabel.setText("OFFLINE [No Status] - " + getEnhancedText());
			shortStatus="[OFFLINE - No Status]";
		} else if (coreStatus.equalsIgnoreCase("ERROR")) {
			componentButton.setStyleName(RED_BUTTON_STYLE);
			popupLabel.setText("Status [ERROR] - Enhanced Admin console");
			shortStatus="[ONLINE - ERROR]";
		} else if (coreStatus.equalsIgnoreCase("WARN")) {
			componentButton.setStyleName(ORANGE_BUTTON_STYLE);
			popupLabel.setText("Status [WARN] - Enhanced Admin console");
			shortStatus="[ONLINE - WARN]";
		} else if (coreStatus.equalsIgnoreCase("NORMAL")) {
			componentButton.setStyleName(GREEN_BUTTON_STYLE);
			popupLabel.setText("Status [NORMAL] - Enhanced Admin console");
			shortStatus="[ONLINE - NORMAL]";
		}
		if(componentLabel!=null){
			componentLabel.setText(coreId+" "+shortStatus);
		}
		componentButton.getElement().setAttribute(
				"aria-label",
				"Shared core: " + coreId + popupLabel.getText());
	}

	public String getCoreStatus() {
		return this.coreStatus;
	}

	public MouseOverHandler onMouseOver = new MouseOverHandler() {
		@Override
		public void onMouseOver(MouseOverEvent event) {
			popup.setPopupPosition(event.getClientX() - 150,
					event.getClientY() + 20);
			popup.setAnimationEnabled(true);
			popup.setAutoHideEnabled(true);
			popup.show();
		}
	};

	public MouseOutHandler onMouseOut = new MouseOutHandler() {
		@Override
		public void onMouseOut(MouseOutEvent arg0) {
			if (popup.isShowing()) {
				popup.hide();
			}
		}
	};

}
