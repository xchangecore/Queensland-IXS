package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Element;

/**
 * CustomDialogBox
 * 
 * DialogBox with a 'x' Button to close it
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class CustomDialogBox extends DialogBox {

	private HorizontalPanel captionPanel = new HorizontalPanel();
	Button closeButton = new Button("X");
	FocusPanel focusPanel = new FocusPanel();

	@SuppressWarnings("deprecation")
	public CustomDialogBox(String title, boolean autoHide) {
		super(true);
		closeButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
				focusPanel.setFocus(false);
			}
		});
		getElement().setAttribute("tabIndex", "1");
		closeButton.setStyleName("closeButton");
		closeButton.setEnabled(true);
		closeButton.setTabIndex(0);
		setCaption(title, closeButton);
	}

	private void setCaption(String txt, Widget w) {
		captionPanel.setWidth("100%");
		focusPanel.add(new HTML(txt));
		focusPanel.setTitle("");
		closeButton.getElement().setAttribute("aria-label",
				"X - to close " + txt);
		focusPanel.getElement().setAttribute("aria-label", "DialogBox");
		if (focusPanel.getElement().getFirstChild() != null)
			((Element) focusPanel.getElement().getFirstChild()).setAttribute(
					"title", txt);
		captionPanel.add(focusPanel);
		captionPanel.add(w);
		captionPanel.setCellHorizontalAlignment(w,
				HasHorizontalAlignment.ALIGN_RIGHT);
		captionPanel.setCellWidth(w, "20%");
		captionPanel.addStyleName("Caption");
		Element td = (Element) this.getCellElement(0, 1);
		td.setInnerHTML("");
		td.appendChild(captionPanel.getElement());
	}

	private class DialogBoxCloseHandler {
		public void onClick(Event event) {
			hide();
			closeButton.setTabIndex(-1);
			focusPanel.setFocus(false);
		}
	}

	protected boolean isCaptionControlEvent(NativeEvent event) {
		return isWidgetEvent(event, closeButton);
	}

	@Override
	protected void onPreviewNativeEvent(NativePreviewEvent event) {
		if (!event.isCanceled() && event.getTypeInt() == Event.ONKEYDOWN) {
			if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
				hide();
				focusPanel.setFocus(false);
			} else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
					&& isCaptionControlEvent(event.getNativeEvent())) {
				hide();
				focusPanel.setFocus(false);
			}
		}
		super.onPreviewNativeEvent(event);
	}

	/**
	 * Overrides the browser event from the DialogBox
	 */
	@Override
	public void onBrowserEvent(Event event) {
		if (isCaptionControlEvent(event)) {

			switch (event.getTypeInt()) {
			case Event.ONMOUSEUP:
			case Event.ONCLICK:
				new DialogBoxCloseHandler().onClick(event);
				break;
			case Event.ONMOUSEOVER:
				break;
			case Event.ONMOUSEOUT:
				break;
			}
			return;
		}
		super.onBrowserEvent(event);
	}

	/**
	 * Function checks if event was inside a given widget
	 * 
	 * @param event
	 *            - current event
	 * @param w
	 *            - widget to prove if event was inside
	 * @return - true if event inside the given widget
	 */
	protected boolean isWidgetEvent(NativeEvent event, Widget w) {
		EventTarget target = event.getEventTarget();

		if (Element.is(target)) {
			boolean t = w.getElement().isOrHasChild(Element.as(target));
			return t;
		}
		return false;
	}

	public void showDialog() {
		center();
		show();
		closeButton.getElement().setAttribute("tabIndex", "0");
		focusPanel.setFocus(true);
	}
}
