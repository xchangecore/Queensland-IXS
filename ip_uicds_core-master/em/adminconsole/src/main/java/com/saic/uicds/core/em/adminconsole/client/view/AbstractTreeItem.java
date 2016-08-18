package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 17/03/2014
 * Time: 2:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractTreeItem extends TreeItem {

    public AbstractTreeItem(String name) {
        super(SafeHtmlUtils.fromString(name));
    }

    public TreeItem addItem(String name) {
        return super.addItem(SafeHtmlUtils.fromString(name));    //To change body of overridden methods use File | Settings | File Templates.
    }
}
