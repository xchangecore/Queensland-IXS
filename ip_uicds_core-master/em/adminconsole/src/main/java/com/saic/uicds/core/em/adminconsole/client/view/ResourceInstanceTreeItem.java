package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

/**
 * ResourceInstanceTreeItem [Performs creation, deletion, and list operations on
 * the ResourceInstance tree root node and child nodes]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.view
 */
public class ResourceInstanceTreeItem extends AbstractTreeItem implements
		UICDSTreeItem, UICDSConstants {

	public static final String rootName = ResourceInstanceFolder;

	private String lastSelectedItem = null;
	private final Map<String, ResourceInstanceGWT> resourceInstanceMap = new TreeMap<String, ResourceInstanceGWT>();
	private final ResourceInstanceCreatePanel createResourceInstancePanel = new ResourceInstanceCreatePanel();
	private final ResourceInstanceEditPanel editResourceInstancePanel = new ResourceInstanceEditPanel();

	public ResourceInstanceTreeItem() {
		super(rootName);
	}

	@Override
	public void cleanUp() {
		resourceInstanceMap.clear();
		this.removeItems();
	}

	public String getLastSelectedItem() {

		return lastSelectedItem;
	}

	public void onLoad() {
		reload();
		getElement().getFirstChildElement().setAttribute("aria-label",
				RESOURCEINSTANCE_DESCRIPTION);
		getElement().getFirstChildElement().setAttribute("aria-live", "off");
		Accessibility.setRole(getElement(), "status");
	}

	@Override
	public void onRightClick(String itemName, int x, int y) {
		if (itemName.equals(rootName)) {
			createResourceInstancePanel.createMenu();
			createResourceInstancePanel.setPopupPosition(x + LocationOffset, y
					+ LocationOffset);
			createResourceInstancePanel.show();
		} else {
			ResourceInstanceGWT resourceInstance = resourceInstanceMap
					.get(itemName);
			if (resourceInstance != null) {
				// menu with option delete ResourceInstance
				editResourceInstancePanel.setPopupPosition(x + LocationOffset,
						y + LocationOffset);
				editResourceInstancePanel.setResourceInstance(resourceInstance);
				editResourceInstancePanel.show();
			}
		}
	}

	public void onSelection(SelectionEvent<TreeItem> e) {
		// set XML content of resource on selection
		String resourceInstacneID = e.getSelectedItem().getText();
		setLastSelectedItem(resourceInstacneID);
		ResourceInstanceGWT resourceInstance = resourceInstanceMap
				.get(resourceInstacneID);
		if (resourceInstance == null) {
			if (resourceInstacneID.equals(CreateResourceInstance)
					|| resourceInstacneID.equals(RemoveResourceInstance)) {
				resourceInstance = resourceInstanceMap.get(e.getSelectedItem()
						.getTitle());
				setXmlDoc(resourceInstance, e);

			} else {
				Util.ERROR("ResourceInstance: " + resourceInstacneID
						+ " not found");
			}
		} else {
			setXmlDoc(resourceInstance, e);
		}
	}

	private void setXmlDoc(ResourceInstanceGWT resourceInstance,
			SelectionEvent<TreeItem> e) {
		final TextArea xmlDocument = ((ExplorerTree) e.getSelectedItem()
				.getTree()).getXmlDocument();
		final HTML htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree())
				.getHtmlDocument();
		htmlDocument.setHTML("");
		Util.setXmlDocument(xmlDocument, resourceInstance.getXML());

	}

	@Override
	public void refreshFolder() {
		cleanUp();
		reload();
	}

	private void refreshLastSelectedItem() {

		if (getLastSelectedItem() != null) {

			ResourceInstanceGWT resourceInstance = resourceInstanceMap
					.get(getLastSelectedItem());
			final TextArea xmlDocument = ((ExplorerTree) this.getTree())
					.getXmlDocument();
			final HTML htmlDocument = ((ExplorerTree) this.getTree())
					.getHtmlDocument();

			if (resourceInstance == null) {
				setLastSelectedItem(null);
				xmlDocument.setText("");
			} else {
				Util.setXmlDocument(xmlDocument, resourceInstance.getXML());
			}
			htmlDocument.setHTML("");
		}
	}

	private void reload() {
		// load all the resourceInstances by calling getList from
		// resourceInstanceService
		BaseController.resourceInstanceServiceProxyAsync
				.getResourceInstanceList(new AsyncCallback<List<ResourceInstanceGWT>>() {
					public void onFailure(Throwable e) {

						Util.DEBUG("Unable to retrieve ResourceInstance XML"
								+ e);
					}

					public void onSuccess(
							List<ResourceInstanceGWT> resourceInstanceList) {
						addItem(SafeHtmlUtils.fromString(CreateResourceInstance));

						for (ResourceInstanceGWT resourceInstance : resourceInstanceList) {
							resourceInstanceMap.put(resourceInstance
									.getIdentifier(), resourceInstance);
						}
						Set<String> resourceIDSet = resourceInstanceMap
								.keySet();
						for (String resourceID : resourceIDSet) {
							// addItem(resourceID);
							TreeItem aTree = addItem(SafeHtmlUtils.fromString(resourceID));
							aTree.setTitle(resourceID);
							aTree.addItem(SafeHtmlUtils.fromString(RemoveResourceInstance)).setTitle(
									resourceID);
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
	public void resourceInstanceCreatePanelFromKeyboard() {
		ResourceInstanceCreatePanel createResourceInstancePanel = new ResourceInstanceCreatePanel(
				true);
	}

	public void resourceInstanceEditPanelHotKeys(String itemName,
			KeyPressEvent e) {
		ResourceInstanceGWT resourceInstance = resourceInstanceMap
				.get(itemName);
		if (resourceInstance != null) {
			editResourceInstancePanel.setResourceInstance(resourceInstance);
		}

		if (e.getCharCode() == 'r' && e.isAltKeyDown()) {
			editResourceInstancePanel.removeResourceInstanceFromKeyboard();
		}

	}

	public void resourceInstanceEditPanelFromKeyboard(String itemName,
			String arg) {
		ResourceInstanceGWT resourceInstance = resourceInstanceMap.get(arg);
		if (resourceInstance != null) {
			editResourceInstancePanel.setResourceInstance(resourceInstance);
		}

		if ((itemName).equals(RemoveResourceInstance)) {
			editResourceInstancePanel.removeResourceInstanceFromKeyboard();
		}

	}

}
