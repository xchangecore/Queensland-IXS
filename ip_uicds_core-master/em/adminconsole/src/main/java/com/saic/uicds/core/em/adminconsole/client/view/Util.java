package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TextArea;

public class Util {

    private final static int MagicScreenSize = 60;

    public static final void DEBUG(String text) {

        Window.alert(text);
    }

    public static void ERROR(String errorMessage) {

        Window.alert(errorMessage);
    }

    public static final int getLineNumber(String content) {

        // return new LineNumberReader(new StringReader(content)).getLineNumber();
        char[] chars = content.toCharArray();
        int numOfLines = 0;
        for (char c : chars) {
            if (c == '\n')
                numOfLines++;
        }

        return numOfLines < MagicScreenSize ? MagicScreenSize : numOfLines + 30;
    }

    public static native void refreshAll() /*-{
                                           $wnd.location.reload();
                                           }-*/;

    public static final void setBlankXmlDocument(TextArea xmlDocument) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < MagicScreenSize; i++) {
            sb.append("\n");
        }
        setXmlDocument(xmlDocument, sb.toString());
    }

    public static final void setXmlDocument(TextArea xmlDocument, String content) {

        xmlDocument.setVisibleLines(Util.getLineNumber(content));
        xmlDocument.setText(content);
    }
}
