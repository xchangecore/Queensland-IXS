package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class AgreementTreeItem extends AbstractTreeItem implements UICDSTreeItem,
		UICDSConstants {

	public static final String rootName = AgreementFolder;

	private String lastSelectedItem = null;
	private final static Map<String, AgreementGWT> agreementMap = new TreeMap<String, AgreementGWT>();
	private final AgreementCreatePanel agreementCreatePanel = new AgreementCreatePanel();
	private final AgreementEditPanel agreementEditPanel = new AgreementEditPanel();

	public AgreementTreeItem() {

		super(rootName);
	}

	public void cleanUp() {

		agreementMap.clear();
		this.removeItems();
	}

	public String getLastSelectedItem() {

		return lastSelectedItem;
	}

	@Override
	public void onLoad() {

		reload();
		getElement().getFirstChildElement().setAttribute("aria-label",
				AGREEMENT_DESCRIPTION);
		getElement().getFirstChildElement().setAttribute("aria-live", "off");
		Accessibility.setRole(getElement(), "status");
	}

	@Override
	public void onRightClick(String itemName, int x, int y) {
		// Window.alert("x: "+x +"y:"+y);
		if (itemName.equals(rootName)) {
			agreementCreatePanel.setPopupPosition(x + LocationOffset, y
					+ LocationOffset);

			agreementCreatePanel.show();

		} else {
			AgreementGWT agreement = agreementMap.get(itemName);
			if (agreement != null) {
				agreementEditPanel.setPopupPosition(x + LocationOffset, y
						+ LocationOffset);
				agreementEditPanel.setAgreement(agreement);
				agreementEditPanel.show();
				// agreementCreatePanel.getElement().setAttribute("tabIndex",
				// "0");
			}
		}

	}

	@Override
	public void agreementCreatePanelHide() {
		agreementCreatePanel.hide();

	}

	public void onSelection(SelectionEvent<TreeItem> e) {

		String agreementID = e.getSelectedItem().getText();
		setLastSelectedItem(agreementID);
		// Util.DEBUG("AgreementFolder: lastSelectedItem: " +
		// getLastSelectedItem());
		AgreementGWT theAgreement = agreementMap.get(agreementID);

		if (theAgreement == null) {
			theAgreement = agreementMap.get(e.getSelectedItem().getTitle());
			if (agreementID.equals(CreateAgreement)
					|| agreementID.equals(ToggleAgreementStatus)
					|| agreementID.equals(AddRule)
					|| agreementID.equals(DeleteRule)
					|| agreementID.equals(RescindAgreement)) {
				setXmlDoc(theAgreement, e);

			} else {
				Window.alert("Agreement: " + agreementID + " not found");
			}
		} else {
			setXmlDoc(theAgreement, e);
		}
	}

	private void setXmlDoc(AgreementGWT theAgreement, SelectionEvent<TreeItem> e) {
		final TextArea xmlDocument = ((ExplorerTree) e.getSelectedItem()
				.getTree()).getXmlDocument();
		final HTML htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree())
				.getHtmlDocument();
		// Window.alert("xmlDocument: "+xmlDocument.toString());
		// Util.DEBUG("AgreementFolder:\n" +
		// theAgreement.getAgreementXml());
		Util.setXmlDocument(xmlDocument, theAgreement.getAgreementXml());
		htmlDocument.setHTML("");

	}

	@Override
	public void refreshFolder() {

		// Util.DEBUG("Refreshing Agreement  Folder ...");
		cleanUp();
		reload();
	}

	private void refreshLastSelectedItem() {

		if (getLastSelectedItem() != null) {

			AgreementGWT agreement = agreementMap.get(getLastSelectedItem());

			final TextArea xmlDocument = ((ExplorerTree) this.getTree())
					.getXmlDocument();
			final HTML htmlDocument = ((ExplorerTree) this.getTree())
					.getHtmlDocument();

			if (agreement == null) {
				setLastSelectedItem(null);
				xmlDocument.setText("");
			} else {
				Util.setXmlDocument(xmlDocument, agreement.getAgreementXml());
			}
			htmlDocument.setHTML("");
		}
	}

	private void reload() {

		BaseController.agreementServiceProxyAsync
				.getAgreementList(new AsyncCallback<List<AgreementGWT>>() {

					public void onFailure(Throwable e) {

						Window.alert("Get list of Agreement failed ...");
					}

					public void onSuccess(List<AgreementGWT> agreementList) {

						cleanUp();
						addItem(CreateAgreement);
						for (AgreementGWT agreement : agreementList) {
							agreementMap.put(agreement.getAgreementTreeTitle(),
									agreement);
						}
						Set<String> agreementIDSet = agreementMap.keySet();
						for (String agreementID : agreementIDSet) {
							// addItem(agreementID);

							TreeItem aTree = addItem(agreementID);
							aTree.setTitle(agreementID);
							aTree.addItem(SafeHtmlUtils.fromString(ToggleAgreementStatus)).setTitle(
                                    agreementID);
							aTree.addItem(SafeHtmlUtils.fromString(AddRule)).setTitle(agreementID);
							aTree.addItem(SafeHtmlUtils.fromString(DeleteRule)).setTitle(agreementID);
							aTree.addItem(SafeHtmlUtils.fromString(RescindAgreement)).setTitle(
									agreementID);

						}
						if (getElement().getElementsByTagName("img")
								.getLength() > 0)
							getElement().getElementsByTagName("img").getItem(0)
									.setAttribute("alt", ALT_TEXT);
						refreshLastSelectedItem();
					}
				});
	}

	public void setLastSelectedItem(String lastSelectedItem) {

		this.lastSelectedItem = lastSelectedItem;
	}

	@Override
	public String type() {

		return rootName;
	}

	// context menu actions from keyboard
	public void agreementCreatePanelFromKeyboard() {
		AgreementCreatePanel agreementCreatePanel = new AgreementCreatePanel(
				true);
	}

	public void agreementEditPanelHotKeys(String itemName, KeyPressEvent e) {
		AgreementGWT agreement = agreementMap.get(itemName);
		if (agreement != null) {
			agreementEditPanel.setAgreement(agreement);
		}

		if (e.getCharCode() == 'o' && e.isAltKeyDown()) {
			agreementEditPanel.toggleFromKeyboard();
		}

		if (e.getCharCode() == 'a' && e.isAltKeyDown()) {
			agreementEditPanel.addRuleFromKeyboard();
		}

		if (e.getCharCode() == 'd' && e.isAltKeyDown()) {
			agreementEditPanel.deleteRuleFromKeyboard();
		}

		if (e.getCharCode() == 'r' && e.isAltKeyDown()) {
			agreementEditPanel.rescindAgrementFromKeyboard();
		}

	}

	public void agreementEditPanelFromKeyboard(String itemName, String agr) {
		AgreementGWT agreement = agreementMap.get(agr);

		if (agreement != null) {
			agreementEditPanel.setAgreement(agreement);

		}

		if ((itemName).equals(ToggleAgreementStatus)) {
			agreementEditPanel.toggleFromKeyboard();
		}

		if ((itemName).equals(AddRule)) {
			agreementEditPanel.addRuleFromKeyboard();
		}

		if ((itemName).equals(DeleteRule)) {
			agreementEditPanel.deleteRuleFromKeyboard();
		}

		if ((itemName).equals(RescindAgreement)) {
			agreementEditPanel.rescindAgrementFromKeyboard();
		}

	}

}
