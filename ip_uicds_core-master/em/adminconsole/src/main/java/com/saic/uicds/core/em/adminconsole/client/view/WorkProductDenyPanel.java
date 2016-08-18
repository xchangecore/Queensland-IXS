package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;

import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class WorkProductDenyPanel extends PopupPanel implements UICDSConstants {

    public WorkProductDenyPanel() {

        super(true, true);

        MenuBar mb = new MenuBar(true);

        setWidget(mb);
        hide();
    }

    public void setMessage(String op, String workProductID, String incidentID) {

        MenuBar mb = (MenuBar) this.getWidget();
        mb.clearItems();
        mb.addItem(op + " Product:[" + workProductID + "] is invalide. Please try to " + op
            + " Incident:[" + incidentID + "] instead.", new Command() {

            @Override
            public void execute() {

                hide();
            }
        });
    }
}
