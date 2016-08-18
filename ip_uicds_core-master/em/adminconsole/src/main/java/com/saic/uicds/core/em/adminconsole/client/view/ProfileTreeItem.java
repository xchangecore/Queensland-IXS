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
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class ProfileTreeItem extends AbstractTreeItem implements UICDSTreeItem,
		UICDSConstants {

	public static final String rootName = ProfileFolder;

	private String lastSelectedItem = null;
	private final Map<String, ProfileGWT> profileMap = new TreeMap<String, ProfileGWT>();
	private final ProfileCreatePanel createProfilePanel = new ProfileCreatePanel();
	private final ProfileEditPanel editProfilePanel = new ProfileEditPanel();

	public ProfileTreeItem() {
		super(rootName);
	}

	@Override
	public void cleanUp() {

		profileMap.clear();
		this.removeItems();
	}

	public String getLastSelectedItem() {

		return lastSelectedItem;
	}

	public void onLoad() {

		reload();
		getElement().getFirstChildElement().setAttribute("aria-label",
				PROFILE_DESCRIPTION);
		getElement().getFirstChildElement().setAttribute("aria-live", "off");
		Accessibility.setRole(getElement(), "status");
	}

	@Override
	public void onRightClick(String itemName, int x, int y) {

		if (itemName.equals(rootName)) {
			createProfilePanel.setPopupPosition(x + LocationOffset, y
					+ LocationOffset);
			createProfilePanel.show();
		} else {
			ProfileGWT profile = profileMap.get(itemName);
			if (profile != null) {
				editProfilePanel.setPopupPosition(x + LocationOffset, y
						+ LocationOffset);
				editProfilePanel.setProfile(profile);
				editProfilePanel.show();
			}
		}
	}

	public void onSelection(SelectionEvent<TreeItem> e) {

		String profileID = e.getSelectedItem().getText();
		setLastSelectedItem(profileID);
		ProfileGWT theProfile = profileMap.get(profileID);

		if (theProfile == null) {
			theProfile = profileMap.get(e.getSelectedItem().getTitle());
			if (profileID.equals(CreateProfile)
					|| profileID.equals(UpdateProfile)
					|| profileID.equals(RemoveProfile)
					|| profileID.equals(AddInterest)
					|| profileID.equals(RemoveInterest)) {
				setXmlDoc(theProfile, e);

			} else {
				// Window.alert(profileID);
				Window.alert("Profile: " + profileID + " not found");
			}
		} else {
			setXmlDoc(theProfile, e);
		}
	}

	private void setXmlDoc(ProfileGWT theProfile, SelectionEvent<TreeItem> e) {
		final TextArea xmlDocument = ((ExplorerTree) e.getSelectedItem()
				.getTree()).getXmlDocument();
		final HTML htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree())
				.getHtmlDocument();
		Util.setXmlDocument(xmlDocument, theProfile.getXML());
		// Util.DEBUG("ProfileFolder: \n" + theProfile.getXML());
		htmlDocument.setHTML("");

	}

	@Override
	public void refreshFolder() {

		cleanUp();
		reload();
	}

	private void refreshLastSelectedItem() {

		if (getLastSelectedItem() != null) {

			ProfileGWT theProfile = profileMap.get(getLastSelectedItem());
			final TextArea xmlDocument = ((ExplorerTree) this.getTree())
					.getXmlDocument();
			final HTML htmlDocument = ((ExplorerTree) this.getTree())
					.getHtmlDocument();

			if (theProfile == null) {
				setLastSelectedItem(null);
				xmlDocument.setText("");
			} else {
				Util.setXmlDocument(xmlDocument, theProfile.getXML());
			}
			htmlDocument.setHTML("");
		}
	}

	private void reload() {

		BaseController.profileServiceProxyAsync
				.getProfileList(new AsyncCallback<List<ProfileGWT>>() {
					public void onFailure(Throwable e) {

						Util.DEBUG("Unable to retrieve XML");
					}

					public void onSuccess(List<ProfileGWT> profileList) {
						addItem(SafeHtmlUtils.fromString(CreateProfile));
						for (ProfileGWT profile : profileList) {
							profileMap.put(profile.getRefName(), profile);
						}
						Set<String> profileIDSet = profileMap.keySet();
						for (String profileID : profileIDSet) {
							// addItem(profileID);

							TreeItem aTree = addItem(SafeHtmlUtils.fromString(profileID));
							aTree.setTitle(profileID);
							aTree.addItem(SafeHtmlUtils.fromString(UpdateProfile)).setTitle(profileID);
							aTree.addItem(SafeHtmlUtils.fromString(RemoveProfile)).setTitle(profileID);
							aTree.addItem(SafeHtmlUtils.fromString(AddInterest)).setTitle(profileID);
							aTree.addItem(SafeHtmlUtils.fromString(RemoveInterest)).setTitle(profileID);
						}
						if (getElement().getElementsByTagName("img")
								.getLength() > 0)
							getElement().getElementsByTagName("img").getItem(0)
									.setAttribute("alt", ALT_TEXT);
						refreshLastSelectedItem();
					}
				});
	}

	public void setLastSelectedItem(String selectedItem) {

		this.lastSelectedItem = selectedItem;
	}

	@Override
	public String type() {

		return rootName;
	}

	@Override
	public void agreementCreatePanelHide() {

	}

	// context menu actions from keyboard
	public void profileCreatePanelFromKeyboard() {
		ProfileCreatePanel createProfilePanel = new ProfileCreatePanel(true);
	}

	public void profileEditPanelHotKeys(String itemName, KeyPressEvent e) {
		ProfileGWT profile = profileMap.get(itemName);
		if (profile != null) {
			editProfilePanel.setProfile(profile);
		}

		if (e.getCharCode() == 'u' && e.isAltKeyDown()) {
			editProfilePanel.updateProfileFromKeyboard();
		}

		if (e.getCharCode() == 'r' && e.isAltKeyDown()) {
			editProfilePanel.removeProfileFromKeyboard();
		}

		if (e.getCharCode() == 'a' && e.isAltKeyDown() && e.isControlKeyDown()) {
			editProfilePanel.addInterestFromKeyboard();
		}

		if (e.getCharCode() == 'r' && e.isAltKeyDown() && e.isControlKeyDown()) {
			editProfilePanel.removeInterestFromKeyboard();
		}

	}

	public void profileEditPanelFromKeyboard(String itemName, String arg) {
		ProfileGWT profile = profileMap.get(arg);
		if (profile != null) {
			editProfilePanel.setProfile(profile);
		}

		if ((itemName).equals(UpdateProfile)) {
			editProfilePanel.updateProfileFromKeyboard();
		}

		if ((itemName).equals(RemoveProfile)) {
			editProfilePanel.removeProfileFromKeyboard();
		}

		if ((itemName).equals(AddInterest)) {
			editProfilePanel.addInterestFromKeyboard();
		}

		if ((itemName).equals(RemoveInterest)) {
			editProfilePanel.removeInterestFromKeyboard();
		}

	}

}
