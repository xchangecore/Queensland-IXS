package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public class WorkProductCloseArchivePanel extends PopupPanel implements UICDSConstants {

    private class CloseArchiveConfirmationCB implements AsyncCallback<String> {

        public void onFailure(Throwable e) {

            Util.ERROR("Close/Archive work product: " + e.getMessage());
        }

        public void onSuccess(String returnMessage) {

            Util.DEBUG(returnMessage);
            DecoratedTabPanel rootTabPanel=(DecoratedTabPanel)RootPanel.get().getWidget(0);
            HorizontalPanel hPanel=(HorizontalPanel)rootTabPanel.getWidget(0);
            HorizontalSplitPanel splitPanel=(HorizontalSplitPanel)hPanel.getWidget(0);
            ExplorerTree tree=(ExplorerTree)splitPanel.getLeftWidget();
            tree.refreshFolder(WorkProductFolder);
        }
    }

    private DialogBox confirmationDialogBox = new DialogBox();
    private Label operationLabel = new Label();
    private Label idLabel = new Label();
    private WorkProductGWT product;

    public WorkProductCloseArchivePanel() {

        super(true, true);

        MenuBar mb = new MenuBar(true);

        initConfirmationDialogBox();

        setWidget(mb);
        hide();
    }

    public WorkProductGWT getProduct() {

        return product;
    }

    public void setProduct(WorkProductGWT product) {

        this.product = product;

        MenuBar mb = (MenuBar) this.getWidget();
        mb.clearItems();

        if (this.product.isClosed()) {
            operationLabel.setText(ArchiveProduct);
            MenuItem menuItem = new MenuItem(ArchiveProduct, new Command() {

                @Override
                public void execute() {

                    confirmationDialogBox.show();
                    hide();
                }
            });
            mb.addItem(menuItem);
        } else {
            operationLabel.setText(CloseProduct);
            MenuItem menuItem = new MenuItem(CloseProduct, new Command() {
                @Override
                public void execute() {

                    confirmationDialogBox.show();
                    hide();
                }
            });
            mb.addItem(menuItem);
        }
        idLabel.setText(product.getProductID());
    }

    private void initConfirmationDialogBox() {

        // String operation = getProduct().isClosed() ? ArchiveProduct : CloseProduct;
        // String productID = getProduct().getProductID();
        String title = "Close/Archive Product Confirmation";
        confirmationDialogBox.setText(title);

        FlexTable layout = new FlexTable();
        layout.setCellSpacing(LayoutCellSpacing);
        FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

        cellFormatter.setColSpan(0, 0, 2);
        cellFormatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);

        // layout.setText(1, 0, operation + " " + productID);
        layout.setWidget(1, 0, operationLabel);
        layout.setText(1, 1, "Product: ");
        layout.setWidget(1, 2, idLabel);

        // Add a ok button at the bottom of the dialog
        Button okButton = new Button(OkButtonText, new ClickHandler() {
            public void onClick(ClickEvent event) {

                if (getProduct().isClosed()) {
                    BaseController.workProductServiceProxyAsync.archiveProduct(
                        getProduct().getProductID(), new CloseArchiveConfirmationCB());
                } else {
                    BaseController.workProductServiceProxyAsync.closeProduct(
                        getProduct().getProductID(), new CloseArchiveConfirmationCB());
                }
                confirmationDialogBox.hide();
            }
        });
        layout.setWidget(3, 1, okButton);

        // Add a cancel button at the bottom of the dialog
        Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
            public void onClick(ClickEvent event) {

                confirmationDialogBox.hide();
            }
        });
        layout.setWidget(3, 2, cancelButton);

        // Wrap the content in a DecoratorPanel
        DecoratorPanel decPanel = new DecoratorPanel();
        decPanel.setWidget(layout);
        confirmationDialogBox.setWidget(decPanel);
        confirmationDialogBox.center();
        confirmationDialogBox.hide();
    }
}
