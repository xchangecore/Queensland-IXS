package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.IGInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.model.InterestGroupGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public class InterestGroupsTreeItem extends AbstractTreeItem implements UICDSTreeItem,
		UICDSConstants {

	private class IncidentComparator implements Comparator<IncidentGWT> {

		@Override
		public int compare(IncidentGWT i1, IncidentGWT i2) {

			return i1.getWorkProductID().compareTo(i2.getWorkProductID());
		}
	}

	private class InterestGroupComparator implements
			Comparator<InterestGroupGWT> {

		@Override
		public int compare(InterestGroupGWT i1, InterestGroupGWT i2) {

			return i1.getInterestGroupType().compareTo(
					i2.getInterestGroupType());
		}
	}

	public static final String rootName = InterestGroupFolder;
	private IncidentCloseArchivePanel incidentCloseArchivePanel = new IncidentCloseArchivePanel();
	private InterestGroupCloseArchivePanel igCloseArchivePanel = new InterestGroupCloseArchivePanel();
	private static final Map<String, Map<String, IncidentGWT>> interestGroupMap = new TreeMap<String, Map<String, IncidentGWT>>();

	private Map<String, List<IncidentGWT>> igInstanceChildWPs = new TreeMap<String, List<IncidentGWT>>();

	ListBox xsltListBox;
	String wpID;
	TextArea xmlDocument;
	HTML htmlDocument;
	private final Map<String, String> previousXsltId = new HashMap<String, String>();
	String xsltDirectory;

	ChangeHandler changeHandler = new ChangeHandler() {
		@Override
		public void onChange(ChangeEvent event) {
			loadWorkProduct();
		}
	};

	public InterestGroupsTreeItem() {

		super(rootName);
	}

	public void cleanUp() {
		interestGroupMap.clear();
		this.removeItems();
	}

	private void handleMenuOnIGType(String itemName, int x, int y) {
		igCloseArchivePanel.setIgType(itemName);
		igCloseArchivePanel.setPopupPosition(x + LocationOffset, y
				+ LocationOffset);
		igCloseArchivePanel.show();
	}

	private void handeMenuOnIGInstances(final IncidentGWT incident,
			final int x, final int y) {
		BaseController.incidentManagementServiceProxyAsync.isIncidentActive(
				incident.getWorkProductID(), new AsyncCallback<Boolean>() {
					@Override
					public void onFailure(Throwable arg0) {
						Util.ERROR("Failed to get status of incident"
								+ arg0.getMessage());
					}

					@Override
					public void onSuccess(Boolean active) {
						boolean isClosed = false;
						if (!active) {
							isClosed = true;
						}
						incidentCloseArchivePanel.setIncidentID(
								incident.getIncidentID(), isClosed);
						incidentCloseArchivePanel.setPopupPosition(x
								+ LocationOffset, y + LocationOffset);
						incidentCloseArchivePanel.show();
					}
				});
	}

	private IncidentGWT handleLeftSideView(SelectionEvent<TreeItem> e) {
		TreeItem tree = e.getSelectedItem();
		// If selected element has children refresh and return GWT object of
		// selected one
		IncidentGWT incidentGWT = refreshTree(tree);
		if (incidentGWT == null) {
			// selected element has no children and it could be a workProduct
			// associated with Instance of IGType
			// retrieve matching workProduct object
			incidentGWT = retreiveIgInstanceChildWP(tree);
		}
		return incidentGWT;
	}

	private void handleRightSideView(SelectionEvent<TreeItem> e,
			IncidentGWT incidentGWT) {
		// code for xslt dropdown values,
		// applying selected xslt to wp, and
		// remembering last selected xslt value
		ExplorerTree etree = (ExplorerTree) e.getSelectedItem().getTree();
		VerticalPanel formPanel = (VerticalPanel) ((DecoratedTabPanel) ((HorizontalSplitPanel) etree
				.getParent()).getRightWidget()).getWidget(0);
		HorizontalPanel xsltPanel = (HorizontalPanel) formPanel.getWidget(0);
		xsltPanel.setVisible(true);

		if (incidentGWT != null) {
			// process xslt view for instance/instanceChild of interestGroupType
			xmlDocument = ((ExplorerTree) e.getSelectedItem().getTree())
					.getXmlDocument();
			htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree())
					.getHtmlDocument();
			htmlDocument.setHTML("");
			xsltListBox = (ListBox) xsltPanel.getWidget(1);
			xsltListBox.setEnabled(true);
			xsltListBox.getElement().setAttribute("aria-label", "List of style sheets available for the selected workProduct");
			xsltListBox.clear();
			wpID = incidentGWT.getWorkProductID();
			loadXsltDropdownAndHtmlContent();
			xsltListBox.addChangeHandler(changeHandler);
			xsltListBox.addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent arg0) {
					loadWorkProduct();
				}
			});
		} else {
			xsltListBox.setEnabled(false);
		}
	}

	/*
	 * load the Instances of InterestGroupType eg:
	 * incidents"A hazarduous chemical event.." for IGType="Incident"
	 */
	private void loadIGInstances(final TreeItem igTree,
			List<IGInstanceGWT> igInstances, final String igType) {

		if (igTree.getChildCount() > 0) {
			igTree.removeItems();
			interestGroupMap.remove(igType);
		}
		Map<String, IncidentGWT> incidentMap = new TreeMap<String, IncidentGWT>();
		for (IGInstanceGWT igInstanceGWT : igInstances) {
			IncidentGWT incident = new IncidentGWT();
			incident.setTitle(igInstanceGWT.getInstanceName());
			incident.setIncidentID(igInstanceGWT.getInterestGroupID());
			incident.setWorkProductID(igInstanceGWT.getWorkProductID());
			incidentMap.put(
					incident.getTitle() + "/" + incident.getIncidentID(),
					incident);
		}
		Set<String> incidentIDSet = incidentMap.keySet();
		for (String incidentID : incidentIDSet) {
			// here is a little bit of tricky in terms of naming
			int index = incidentID.indexOf("/");
			String name = incidentID.substring(0, index);
			TreeItem iTree = igTree.addItem(SafeHtmlUtils.fromString(name));
			iTree.setTitle(incidentID);
			HTML html = new HTML(
					"Select Incident to retrieve its work products");
			html.setStyleName("childNode");
			iTree.addItem(html);
			if(iTree.getElement().getElementsByTagName("img").getLength()>0)
				iTree.getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
		}
		if(igTree.getElement().getElementsByTagName("img").getLength()>0)
			igTree.getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
		interestGroupMap.put(igType, incidentMap);
	}

	public void loadWorkProduct() {
		int index = xsltListBox.getSelectedIndex();
		String xsltId = xsltListBox.getItemText(index);
		previousXsltId.put(wpID, xsltId);
		boolean showDefaultWP = false;
		if (xsltId.equals("default")) {
			showDefaultWP = true;
		}
		if (index == 0) {
			xsltId = "default:" + xsltId;
		}
		BaseController.workProductServiceProxyAsync.getProduct(wpID, xsltId,
				showDefaultWP, new AsyncCallback<WorkProductGWT>() {
					public void onFailure(Throwable e) {

						Window.alert("Cannot get work product: "
								+ e.getMessage());
					}

					public void onSuccess(WorkProductGWT result) {

						Util.setXmlDocument(xmlDocument, result.getProduct());
						htmlDocument.setHTML(result.getProductHtml());
					}
				});
	}

	/*
	 * load workProducts associated with an instance of IGType
	 */
	private void loadWorkProducts(final TreeItem incidentTree,
			final String igType, final String incidentId) {
		if (incidentTree.getChildCount() > 0) {
			incidentTree.removeItems();
		}
		BaseController.interestGroupManagementServiceProxyAsync
				.getAssociatedWorkProducts(igType, incidentId,
						new AsyncCallback<List<IncidentGWT>>() {
							@Override
							public void onFailure(Throwable arg0) {

							}

							@Override
							public void onSuccess(
									List<IncidentGWT> workProductsGwt) {
								igInstanceChildWPs.put(incidentId,
										workProductsGwt);
								TreeSet<IncidentGWT> sortedChildren = new TreeSet<IncidentGWT>(
										new IncidentComparator());
								for (IncidentGWT workProductGwt : workProductsGwt) {
									sortedChildren.add(workProductGwt);
								}

								for (IncidentGWT child : sortedChildren) {
									incidentTree.addItem(SafeHtmlUtils.fromString(child.getWorkProductID()));
								}
								if(incidentTree.getElement().getElementsByTagName("img").getLength()>0)
									incidentTree.getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
							}
						});
	}

	private void loadWpWithXslt(String id, boolean showDefaultWP) {
		BaseController.workProductServiceProxyAsync.getProduct(wpID, id,
				showDefaultWP, new AsyncCallback<WorkProductGWT>() {
					public void onFailure(Throwable e) {
						Window.alert("Cannot get work product: "
								+ e.getMessage());
					}

					public void onSuccess(WorkProductGWT result) {
						Util.setXmlDocument(xmlDocument, result.getProduct());
						htmlDocument.setHTML(result.getProductHtml());
					}
				});
	}

	private void loadXsltDropdownAndHtmlContent() {
		BaseController.workProductServiceProxyAsync.getProductXsltIds(wpID,
				new AsyncCallback<List<String>>() {
					private void handleXslts(List<String> xsltIds) {
						String prevId = previousXsltId.get(wpID);
						boolean contains = false;
						if (prevId == null) {
							prevId = xsltIds.get(0).replace("default:", "");
							previousXsltId.put(wpID, prevId);
							xsltListBox.setSelectedIndex(0);
							contains = true;
						}
						for (String xsltId : xsltIds) {
							if (xsltId.contains("default:")) {
								xsltId = xsltId.replace("default:", "");
							}
							xsltListBox.addItem(xsltId);
							if (prevId.equals(xsltId)) {
								xsltListBox.setSelectedIndex(xsltListBox
										.getItemCount() - 1);
								contains = true;
							}
						}
						xsltListBox.setVisibleItemCount(1);
						if (!contains) {
							xsltListBox.setSelectedIndex(0);
							previousXsltId.put(wpID, xsltListBox.getItemText(0));
						}
						String id = previousXsltId.get(wpID);
						boolean showDefaultWP = false;
						if (id.equals("default")) {
							showDefaultWP = true;
						}
						if (id.equals(xsltListBox.getItemText(0))) {
							showDefault(showDefaultWP);
						} else {
							loadWpWithXslt(id, showDefaultWP);
						}
					}

					public void onFailure(Throwable e) {
						Window.alert("Cannot get XSLT Ids: " + e.getMessage());
					}

					public void onSuccess(List<String> xsltIds) {
						xsltListBox.clear();
						if (xsltIds.size() > 0) {
							handleXslts(xsltIds);
						} else {
							int ind = xsltDirectory.lastIndexOf("ServerApps");
							String dir = xsltDirectory.substring(0, ind);
							Util.ERROR("No default xslt found: default.xsl file is missing from directory "
									+ xsltDirectory
									+ ". Copy the file from "
									+ dir + "InstallFiles\\conf\\ directory");
						}
					}

				});
	}

	public void onLoad() {
		reload();
		getElement().getFirstChildElement().setAttribute("aria-label", INTERESTGROUP_DESCRIPTION);
		getElement().getFirstChildElement().setAttribute("aria-live", "off");
        Accessibility.setRole(getElement(), "status");
	}

	@Override
	public void onRightClick(String itemName, final int x, final int y) {
		IncidentGWT incidentGWT = null;
		for (String igType : interestGroupMap.keySet()) {
			Map<String, IncidentGWT> igInstancesMap = interestGroupMap
					.get(igType);
			if (igInstancesMap.keySet().contains(itemName)) {
				incidentGWT = igInstancesMap.get(itemName);
			}
		}
		final IncidentGWT incident = incidentGWT;
		if (incident != null) {
			handeMenuOnIGInstances(incident, x, y);
		} else {
			// TODO: catch Right click on IGType => "Close/Archive ALL"
			if (interestGroupMap.keySet().contains(itemName)) {
				handleMenuOnIGType(itemName, x, y);
			}
		}
	}

	public void onSelection(SelectionEvent<TreeItem> e) {
		IncidentGWT incidentGWT = handleLeftSideView(e); // Tree view
		handleRightSideView(e, incidentGWT);
	}

	@Override
	public void refreshFolder() {
		cleanUp();
		reload();
	}

	private void refreshIgInstancesTree(TreeItem tree, String igType, String key) {
		if (igType.equalsIgnoreCase("Incident")) {
			String incidentId = key.substring(key.lastIndexOf("/") + 1);
			loadWorkProducts(tree, igType, incidentId);
			// loadIncidentWorkProducts(tree, igType, incidentId);
		}
	}

	private void refreshIgTree(final TreeItem rootIgTree, final String key) {
//		if (key.equals("Incident")) {
			BaseController.interestGroupManagementServiceProxyAsync
					.getListOfIGInstances(key,
							new AsyncCallback<InterestGroupGWT>() {
								@Override
								public void onFailure(Throwable arg0) {
								}

								@Override
								public void onSuccess(
										InterestGroupGWT interestGroupGWT) {
									loadIGInstances(rootIgTree,
											interestGroupGWT.getIgInstances(),
											key);
								}
							});
//		}
	}

	private IncidentGWT refreshTree(TreeItem tree) {
		String key = tree.getTitle();
		IncidentGWT incidentGWT = null;
		if (interestGroupMap.keySet().contains(key)) {
			// selected element is InterestGroupsType
			// refresh ListOfInstances for InterestGroupType say "Incident"
			refreshIgTree(tree, key);
		} else {
			for (String igType : interestGroupMap.keySet()) {
				Map<String, IncidentGWT> igInstancesMap = interestGroupMap
						.get(igType);
				if (igInstancesMap.keySet().contains(key)) {
					// selected element is Instance of InterestGroupType
					// refresh ListOfWorkproducts associated with InterestGroup
					// Instance
					refreshIgInstancesTree(tree, igType, key);
					incidentGWT = igInstancesMap.get(key);
				}
			}
		}
		return incidentGWT;
	}

	private void reload() {
		BaseController.interestGroupManagementServiceProxyAsync
				.getListOfInterestGroupsWithInstances(new AsyncCallback<List<InterestGroupGWT>>() {
					private void addItemToIGTree(InterestGroupGWT interestGroup) {
						TreeItem igTree = addItem(SafeHtmlUtils.fromString(interestGroup.getInterestGroupType()));
						igTree.setTitle(interestGroup.getInterestGroupType());
						loadIGInstances(igTree, interestGroup.getIgInstances(),
								interestGroup.getInterestGroupType());
					}

					@Override
					public void onFailure(Throwable arg0) {
					}

					@Override
					public void onSuccess(List<InterestGroupGWT> igWithInstances) {
						TreeSet<InterestGroupGWT> sortedInterestGroups = new TreeSet<InterestGroupGWT>(
								new InterestGroupComparator());

						for (InterestGroupGWT interestGroup : igWithInstances) {
							sortedInterestGroups.add(interestGroup);
							interestGroupMap.put(
									interestGroup.getInterestGroupType(),
									new TreeMap<String, IncidentGWT>());
						}
						for (InterestGroupGWT interestGroup : sortedInterestGroups) {
							addItemToIGTree(interestGroup);
						}
						if(getElement().getElementsByTagName("img").getLength()>0)
							getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
					}
				});
	}

	/*
	 * This returns the workproduct associated with the instance of a particular
	 * InterestGroupType eg: Incidet/ICS/Map associated with
	 * Incident:"A Hazardous Chemical Event occured" of
	 * InterestGroupType:"Incident"
	 */
	private IncidentGWT retreiveIgInstanceChildWP(TreeItem tree) {
		// Get the instances of IGType:"Incident" or other
		// Iterate over Instances of say "Incident"
		for (String instanceChild : igInstanceChildWPs.keySet()) {
			List<IncidentGWT> workProducts = igInstanceChildWPs
					.get(instanceChild);
			// Get the workProducts associated with Instance of say
			// IGType:"Incident" or other
			// Iterate and return matching selected element
			for (IncidentGWT wp : workProducts) {
				if (tree.getText().equalsIgnoreCase(wp.getWorkProductID()))
					return wp;
			}
		}
		return null;
	}

	public void setXsltDirectory(String url) {
		this.xsltDirectory = url;
	}

	private void showDefault(boolean showDefaultWP) {
		BaseController.workProductServiceProxyAsync.getProduct(wpID,
				showDefaultWP, new AsyncCallback<WorkProductGWT>() {
					public void onFailure(Throwable e) {
						Window.alert("Cannot get the Incident: "
								+ e.getMessage());
					}

					public void onSuccess(WorkProductGWT result) {
						Util.setXmlDocument(xmlDocument, result.getProduct());
						htmlDocument.setHTML(result.getProductHtml());
					}
				});
		previousXsltId.put(wpID, xsltListBox.getItemText(0));
	}

	@Override
	public String type() {

		return rootName;
	}

	 @Override
		public void agreementCreatePanelHide(){
			
		}

}
