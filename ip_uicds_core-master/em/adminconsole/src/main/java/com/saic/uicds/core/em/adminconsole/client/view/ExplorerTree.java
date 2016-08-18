package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;

public class ExplorerTree extends Tree implements UICDSConstants {

	public static final String rootName = UICDSExplorer;
	private final static Map<String, UICDSTreeItem> treeItems = new HashMap<String, UICDSTreeItem>();
	private final TreeItem root;
	private final TextArea xmlDocument;
	private final HTML htmlDocument;
	private static UICDSTreeItem selectedUICDSTreeItem;
	private static String selectedItemName;
	private static String selectedTitle;
	private static String incidentRssFeedUrl = null;
	private final boolean isAdmin;
	private static TreeItem tItem;

	public boolean isAdmin() {

		return isAdmin;
	}

	@SuppressWarnings("deprecation")
	public ExplorerTree(TextArea xmlDocument, final HTML htmlDocument,
			boolean isAdmin) {
		this.xmlDocument = xmlDocument;
		this.htmlDocument = htmlDocument;
		this.isAdmin = isAdmin;
		setTabIndex(0);
		root = addItem(SafeHtmlUtils.fromString(rootName));
		Accessibility.setRole(root.getElement(), Accessibility.ROLE_TREEITEM);
		Accessibility.setState(root.getElement(), Accessibility.STATE_EXPANDED,
				"true");

		CoreTreeItem coreTree = new CoreTreeItem();
		root.addItem(coreTree);
		treeItems.put(CoreTreeItem.rootName, coreTree);

		ServiceTreeItem serviceTree = new ServiceTreeItem();
		root.addItem(serviceTree);
		treeItems.put(ServiceTreeItem.rootName, serviceTree);

		final AgreementTreeItem agreementTree = new AgreementTreeItem();
		root.addItem(agreementTree);
		treeItems.put(AgreementTreeItem.rootName, agreementTree);
		

		final ProfileTreeItem profileTree = new ProfileTreeItem();
		root.addItem(profileTree);
		treeItems.put(ProfileTreeItem.rootName, profileTree);

		InterestGroupsTreeItem interestGroupsTree = new InterestGroupsTreeItem();
		root.addItem(interestGroupsTree);
		treeItems.put(InterestGroupsTreeItem.rootName, interestGroupsTree);

		// IncidentTreeItem incidentTree = new IncidentTreeItem();
		// root.addItem(incidentTree);
		// treeItems.put(IncidentTreeItem.rootName, incidentTree);

		WorkProductTreeItem workProductTree = new WorkProductTreeItem();
		root.addItem(workProductTree);
		treeItems.put(WorkProductTreeItem.rootName, workProductTree);

		final ResourceInstanceTreeItem resourceInstanceTree = new ResourceInstanceTreeItem();
		root.addItem(resourceInstanceTree);
		treeItems.put(ResourceInstanceTreeItem.rootName, resourceInstanceTree);

		addOpenHandler(new OpenHandler<TreeItem>() {
			@Override
			public void onOpen(OpenEvent<TreeItem> e) {
				String item = e.getTarget().getText();
				UICDSTreeItem treeItem = treeItems.get(item);
				if (treeItem != null)
					treeItem.refreshFolder();
			}
		});

		addSelectionHandler(new SelectionHandler<TreeItem>() {
			@Override
			public void onSelection(SelectionEvent<TreeItem> event) {

				VerticalPanel formPanel = (VerticalPanel) ((DecoratedTabPanel) ((HorizontalSplitPanel) getParent())
						.getRightWidget()).getWidget(0);
				formPanel.getWidget(0).setVisible(false);
				formPanel.getWidget(2).setVisible(false);

				TreeItem selectedItem = event.getSelectedItem();
				tItem = selectedItem;
				selectedItemName = selectedItem.getText();
				if (selectedItemName.equalsIgnoreCase(IncidentRSSFeed))
					return;
				// only incident tree item has title as the work product ID
				selectedTitle = selectedItem.getTitle();

				htmlDocument.setText("");
				selectedUICDSTreeItem = treeItems.get(selectedItemName);

				if (selectedItemName.equals("Incidnet")) {
					selectedUICDSTreeItem = treeItems.get(InterestGroupFolder);
				}
				if (selectedUICDSTreeItem == null) {
					String parentItemName = selectedItemName;
					while (selectedUICDSTreeItem == null) {
						selectedItem = selectedItem.getParentItem();
						if (selectedItem == null)
							break;
						parentItemName = selectedItem.getText();
						selectedUICDSTreeItem = treeItems.get(parentItemName);
					}
					if (selectedUICDSTreeItem != null) {
						if (parentItemName.equals(ServiceFolder)
								|| parentItemName.equals(ProfileFolder)
								|| parentItemName.equals(AgreementFolder)
								|| parentItemName
										.equals(ResourceInstanceFolder)) {
							((DecoratedTabPanel) ((HorizontalSplitPanel) getParent())
									.getRightWidget()).selectTab(XmlViewIndex);
						} else {
							((DecoratedTabPanel) ((HorizontalSplitPanel) getParent())
									.getRightWidget()).selectTab(FormViewIndex);
						}
						selectedUICDSTreeItem.onSelection(event);
					}
				} else {
					// reload the folder
					// access the xsltDropDown and make visible but disabled at
					// this level
					HorizontalPanel xsltPanel = (HorizontalPanel) formPanel
							.getWidget(0);
					ListBox list = (ListBox) xsltPanel.getWidget(1);
					list.setEnabled(false);
					// if (selectedItemName.equals(IncidentFolder)) {
					// xsltPanel.setVisible(true);
					// } else if (selectedItemName.equals(WorkProductFolder)) {
					// xsltPanel.setVisible(true);
					// } else if (selectedItemName.equals(InterestGroupFolder))
					// {
					// xsltPanel.setVisible(true);
					// }
					selectedUICDSTreeItem.refreshFolder();
					Accessibility.setRole(selectedItem.getElement(),
							Accessibility.ROLE_TREEITEM);
					Accessibility.setState(selectedItem.getElement(),
							Accessibility.STATE_SELECTED, "true");
					Accessibility.setState(selectedItem.getElement(),
							Accessibility.STATE_ACTIVEDESCENDANT, DOM
									.getElementAttribute(selectedItem
											.getElement(), "id"));
					Accessibility.setRole(selectedItem.getElement(), "status");
				}
			}

		});

		addKeyPressHandler(new KeyPressHandler() {

			@Override
			public void onKeyPress(KeyPressEvent e) {
				//Window.alert("e.getCharCode(): "+e.getCharCode() +"KeyCode"+e.getNativeEvent().getKeyCode()+"selectedItemName"+selectedItemName.toString());
				//Window.alert("selectedUICDSTreeItem.type(): "+selectedUICDSTreeItem.type());
				//Window.alert(selectedTitle);
				//Agreements
				if((selectedItemName).equals(CreateAgreement) && e.getNativeEvent().getKeyCode()==13){
					agreementTree.agreementCreatePanelFromKeyboard();
				}
				if(((selectedItemName).equals(ToggleAgreementStatus) || 
						(selectedItemName).equals(AddRule) || 
						(selectedItemName).equals(DeleteRule) || 
						(selectedItemName).equals(RescindAgreement)) && e.getNativeEvent().getKeyCode()==13){
							setTabIndex(-1);
							agreementTree.agreementEditPanelFromKeyboard(selectedItemName, selectedTitle);
						}
				//Profiles
				if((selectedItemName).equals(CreateProfile) && e.getNativeEvent().getKeyCode()==13){
					profileTree.profileCreatePanelFromKeyboard();
				}
			
				if(((selectedItemName).equals(UpdateProfile) || 
						(selectedItemName).equals(RemoveProfile) || 
						(selectedItemName).equals(AddInterest) || 
						(selectedItemName).equals(RemoveInterest)) && e.getNativeEvent().getKeyCode()==13){
					profileTree.profileEditPanelFromKeyboard(selectedItemName, selectedTitle);
						}
				
				//ResourceInstance
				if((selectedItemName).equals(CreateResourceInstance) && e.getNativeEvent().getKeyCode()==13){
					resourceInstanceTree.resourceInstanceCreatePanelFromKeyboard();
				}
			
				if((selectedItemName).equals(RemoveResourceInstance)
						&& e.getNativeEvent().getKeyCode()==13){
					resourceInstanceTree.resourceInstanceEditPanelFromKeyboard(selectedItemName, selectedTitle);
						}
				
				//hotKeys
				if (selectedUICDSTreeItem.type().equals(AgreementFolder)) {
					if (e.getCharCode() == 'c' && e.isAltKeyDown() && selectedItemName.equals(AgreementFolder)) {
						agreementTree.agreementCreatePanelFromKeyboard();
					} else {
						agreementTree.agreementEditPanelHotKeys(selectedItemName, e);
					}
				} else if (selectedUICDSTreeItem.type().equals(ProfileFolder)) {
					if (e.getCharCode() == 'c' && e.isAltKeyDown() && selectedItemName.equals(ProfileFolder)) {
						profileTree.profileCreatePanelFromKeyboard();
					} else {
						profileTree.profileEditPanelHotKeys(selectedItemName, e);
					}
				} else if (selectedUICDSTreeItem.type().equals(ResourceInstanceFolder)) {
					if (e.getCharCode() == 'c' && e.isAltKeyDown() && selectedItemName.equals(ResourceInstanceFolder)) {
						resourceInstanceTree.resourceInstanceCreatePanelFromKeyboard();
					} else {
						resourceInstanceTree.resourceInstanceEditPanelHotKeys(selectedItemName, e);
					}
				}
			}
		});

		addKeyboardListener(new KeyboardListener() {

			@Override
			public void onKeyUp(Widget arg0, char arg1, int arg2) {

			}

			@Override
			public void onKeyPress(Widget arg0, char arg1, int arg2) {
				Tree tree = (Tree) arg0;
				String text = tree.getSelectedItem().getText();
				if (arg1 == KEY_ENTER && text.equalsIgnoreCase(IncidentRSSFeed)) {
					Window.Location.assign(incidentRssFeedUrl);
				}
			}

			@Override
			public void onKeyDown(Widget arg0, char arg1, int arg2) {

			}
		});
		addMouseUpHandler(new MouseUpHandler() {

			@Override
			public void onMouseUp(MouseUpEvent e) {
				if (e.getNativeButton() == NativeEvent.BUTTON_RIGHT) {
					if (isAdmin()) {
						if (selectedUICDSTreeItem != null
								&& selectedUICDSTreeItem.type().equals(
										InterestGroupFolder)) {
							selectedUICDSTreeItem.onRightClick(selectedTitle, e
									.getClientX(), e.getClientY());
						}
					}
				}
			}
			
		});
		// the mouse handler to handle the right click
		addMouseDownHandler(new MouseDownHandler() {

			@Override
			public void onMouseDown(MouseDownEvent e) {

				if (e.getNativeButton() == NativeEvent.BUTTON_RIGHT) {
					if (isAdmin()) {
						if (selectedUICDSTreeItem != null) {
							if (selectedUICDSTreeItem.type().equals(
									InterestGroupFolder)) {
								// if selection is Incident then call
								// IncidentTreeItem.onRightClick
								if (selectedTitle != null
										&& selectedTitle.trim().length() != 0) {
									selectedUICDSTreeItem.onRightClick(
											selectedTitle, e.getClientX(), e
													.getClientY());
								} else {
									Collection<UICDSTreeItem> items = treeItems
											.values();
									for (UICDSTreeItem item : items) {
										if (item.type().equals(
												WorkProductFolder)) {
											item.onRightClick(selectedItemName,
													e.getClientX(), e
															.getClientY());
											break;
										}
									}
								}
							} else {
								selectedUICDSTreeItem.onRightClick(
										selectedItemName, e.getClientX(), e
												.getClientY());
							}
						}
					}
				} else if (e.getNativeButton() == NativeEvent.BUTTON_LEFT) {
					if (selectedItemName != null
							&& selectedItemName
									.equalsIgnoreCase(IncidentRSSFeed)) {
						Window.Location.assign(incidentRssFeedUrl);
					}
				}
			}
			
			
		});
		
		addDomHandler(new DoubleClickHandler() {
			 @Override
		        public void onDoubleClick(DoubleClickEvent e) {
		            
					//Agreements
					if((selectedItemName).equals(CreateAgreement)){
						agreementTree.agreementCreatePanelFromKeyboard();
					}
					if(((selectedItemName).equals(ToggleAgreementStatus) || 
							(selectedItemName).equals(AddRule) || 
							(selectedItemName).equals(DeleteRule) || 
							(selectedItemName).equals(RescindAgreement))){
								setTabIndex(-1);
								agreementTree.agreementEditPanelFromKeyboard(selectedItemName, selectedTitle);
							}
					//Profiles
					if((selectedItemName).equals(CreateProfile)){
						profileTree.profileCreatePanelFromKeyboard();
					}
				
					if(((selectedItemName).equals(UpdateProfile) || 
							(selectedItemName).equals(RemoveProfile) || 
							(selectedItemName).equals(AddInterest) || 
							(selectedItemName).equals(RemoveInterest))){
						profileTree.profileEditPanelFromKeyboard(selectedItemName, selectedTitle);
							}
					
					//ResourceInstance
					if((selectedItemName).equals(CreateResourceInstance)){
						resourceInstanceTree.resourceInstanceCreatePanelFromKeyboard();
					}
				
					if((selectedItemName).equals(RemoveResourceInstance)){
						resourceInstanceTree.resourceInstanceEditPanelFromKeyboard(selectedItemName, selectedTitle);
							}
		        }
		    }, DoubleClickEvent.getType());


		ensureSelectedItemVisible();
		root.setState(true);
	}

	public HTML getHtmlDocument() {

		return htmlDocument;
	}

	public TextArea getXmlDocument() {

		return xmlDocument;
	}

	@Override
	protected void onLoad() {

		if (incidentRssFeedUrl == null) {
			BaseController.directoryServiceProxyAsync
					.getRequestUrl(new AsyncCallback<String>() {
						public void onFailure(Throwable e) {

							Util.ERROR("Cannot get the core name: "
									+ e.getMessage());
						}

						public void onSuccess(String url) {

							incidentRssFeedUrl = new String(url
									+ IncidentRSSFeedUrl);

							// Util.DEBUG("IncidentRssFeedUrl: " +
							// incidentRssFeedUrl);
							// incident rss feed link

							TreeItem rssAnchor = root.addItem(SafeHtmlUtils.fromString(IncidentRSSFeed));
							rssAnchor.setStyleName("anchorUnderline");
							// TreeItem item=root.addItem(new
							// HTML("<html><a href=\""
							// + incidentRssFeedUrl + "\">"
							// + IncidentRSSFeed +
							// " tabindex=\"-1\" </a></html>"));
							// DOM.setIntAttribute(item.getElement(),
							// "tabIndex", -1);
						}
					});
		}
		BaseController.workProductServiceProxyAsync
				.getXsltConfiguredDirectory(new AsyncCallback<String>() {
					public void onFailure(Throwable e) {
						Util.ERROR("Cannot get the configured direcotry: "
								+ e.getMessage());
					}

					public void onSuccess(String url) {
						WorkProductTreeItem wpTree = (WorkProductTreeItem) treeItems
								.get(WorkProductFolder);
						wpTree.setXsltDirectory(url);
						// IncidentTreeItem incTree=(IncidentTreeItem)
						// treeItems.get(IncidentFolder);
						// incTree.setXsltDirectory(url);
						InterestGroupsTreeItem igTree = (InterestGroupsTreeItem) treeItems
								.get(InterestGroupFolder);
						igTree.setXsltDirectory(url);

					}
				});
		Collection<UICDSTreeItem> items = treeItems.values();
		for (UICDSTreeItem item : items) {
			item.onLoad();
		}

		Node node = root.getElement().getFirstChild().getFirstChild()
				.getFirstChild();
		Element elem = (Element) node.getChildNodes().getItem(1)
				.getFirstChild();
		elem
				.setAttribute(
						"aria-label",
						"Uicds v1.2.0 Explorer: Root TreeItem contains List of core components as treeitems");
		elem.setAttribute("aria-live", "assertive");
		Accessibility.setRole(getElement(), "status");
	}

	public void refreshFolder(String treeItem) {

		// Util.DEBUG("refresh " + treeItem);
		treeItems.get(treeItem).refreshFolder();
		if (treeItem.equals(InterestGroupFolder))
			treeItems.get(WorkProductFolder).refreshFolder();
		else if (treeItem.equals(IncidentFolder))
			treeItems.get(WorkProductFolder).refreshFolder();
		else if (treeItem.equals(WorkProductFolder))
			treeItems.get(InterestGroupFolder).refreshFolder();
		// treeItems.get(IncidentFolder).refreshFolder();
	}
}
