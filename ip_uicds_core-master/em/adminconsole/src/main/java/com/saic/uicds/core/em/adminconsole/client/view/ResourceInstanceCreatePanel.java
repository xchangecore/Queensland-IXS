package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

/**
 * ResourceInstanceCreatePanel [Popup Panel with options: {Create} ]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.view
 */

public class ResourceInstanceCreatePanel extends PopupPanel implements
		UICDSConstants {

	private final ResourceInstanceEditDialog createResourceInstanceDialog = new ResourceInstanceEditDialog(
			CreateResourceInstance);

	public ResourceInstanceCreatePanel() {

		super(true, true);
		hide();
	}

	public void createMenu() {
		// Create the edit menu
		MenuBar createMenu = new MenuBar(true);
		MenuItem menuItem = new MenuItem(CreateResourceInstance, new Command() {
			@Override
			public void execute() {
				createResourceInstanceDialog
						.createResourceInstanceDialogItems();
				createResourceInstanceDialog.showDialog();
				hide();
			}
		});
		createMenu.addItem(menuItem);
		setWidget(createMenu);
		hide();
	}

	public ResourceInstanceCreatePanel(boolean keyboard) {

		super(true, true);
		createResourceInstanceDialog.createResourceInstanceDialogItems();
		createResourceInstanceDialog.showDialog();
	}

}
