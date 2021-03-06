package org.openstreetmap.josm.plugins.scripting.ui.console;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;

import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;

public class ScriptingConsolePanelTest extends JFrame {

    private ScriptingConsolePanel console;

    protected void build() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(console = new ScriptingConsolePanel(), BorderLayout.CENTER);

        setSize(600,800);
    }

    public ScriptingConsolePanelTest() {
        build();
    }

    static public void main(String args[]) {
        ProjectionPreference.setProjection("core:meractor", null /* no prefs */, true /* default projection */);
        ScriptingConsolePanelTest app = new ScriptingConsolePanelTest();
        app.setVisible(true);
    }
}
