package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.Widget;

public class UicdsExplorerPanel extends HorizontalPanel {

    private Tree tree;
    public final TextArea xmlDocument = new TextArea();
    private final HTML htmlDocument = new HTML();

    public UicdsExplorerPanel() {
        tree = new ExplorerTree(xmlDocument, htmlDocument, true);
        add(tree);
        add(createServiceElementsTab());
    }
    
    private Widget createServiceElementsTab() {
        DecoratedTabPanel tabPanel = new DecoratedTabPanel();
        xmlDocument.setReadOnly(true);
        xmlDocument.setStylePrimaryName("gwt-TextArea-readonly");
        Util.setBlankXmlDocument(xmlDocument);
        tabPanel.add(htmlDocument, "Form");
        tabPanel.add(xmlDocument, "XML");
        tabPanel.selectTab(0);
        tabPanel.setAnimationEnabled(true);
        tabPanel.setSize("100%", "100%");
        tabPanel.setStylePrimaryName("gwt-TabPanelBottom");
        return tabPanel;
    }

}
