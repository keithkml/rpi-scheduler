/*
 *  Copyright (c) 2004, The University Scheduler Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the University Scheduler Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.rpi.scheduler.ui.savesched;

import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.WindowType;
import edu.rpi.scheduler.ui.MonitoredJFrame;
import edu.rpi.scheduler.ui.panels.view.ScheduleViewer;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class LoadedScheduleWindow extends MonitoredJFrame {
    private static final Preferences PREFERENCES
            = Preferences.userNodeForPackage(LoadedScheduleWindow.class);
    private static final String KEY_SCHEDULE_WINDOW = "load-schedule-window";
    private JButton printButton;

    private static Icon getWarningIcon() {
        return UIManager.getIcon("OptionPane.warningIcon");
    }


    private JPanel mainPanel;
    private ScheduleViewer scheduleViewer;
    private JTextPane infoBox;
    private JPanel infoPanel;
    private JLabel iconLabel;
    private JScrollPane infoBoxPane;

    private SchedulingSession session;

    private PrintScheduleAction printAction = new PrintScheduleAction();

    {
        getContentPane().add(mainPanel);

        UITools.makeTextPaneLookLikeDialog(infoBox);
        infoBoxPane.setBorder(new EmptyBorder(0,0,0,0));

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                UITools.saveWindowPositionAndSize(e.getWindow(),
                        PREFERENCES, KEY_SCHEDULE_WINDOW);
            }
        });

        printButton.setAction(printAction);
    }

    public LoadedScheduleWindow(SchedulingSession session) throws HeadlessException {
        this.session = session;
        SchedulerUIPlugin plugin = session.getUIPlugin();
        setIconImage(plugin.getWindowIcon(WindowType.VIEW_SCHEDULE));
        scheduleViewer.setSession(session);
        scheduleViewer.init();
    }

    public ScheduleViewer getScheduleViewer() {
        return scheduleViewer;
    }

    public void setMessageIcon(Icon icon) {
        iconLabel.setIcon(icon);
    }

    public void setMessage(String s) {
        infoPanel.setVisible(s != null);
        infoBox.setText(s);
    }

    public void setSchedule(Window parent, LoadedSchedule scheduleInfo,
            File file) {
        if (!scheduleInfo.wasLoadedFlawlessly()) {
            setMessageIcon(getWarningIcon());
            setMessage("This schedule may not have been loaded "
                    + "completely due to errors in the saved schedule "
                    + "file.");
        }

        scheduleViewer.setSchedule(scheduleInfo.getSchedule());

        if (!UITools.loadWindowPosition(this, PREFERENCES, KEY_SCHEDULE_WINDOW)) {
            setLocationRelativeTo(parent);
        }
        if (!UITools.loadWindowSize(this, PREFERENCES, KEY_SCHEDULE_WINDOW)) {
            SchedulerUIPlugin plugin = session.getUIPlugin();
            setSize(plugin.getDefaultWindowSize(WindowType.VIEW_SCHEDULE));
        }

        File bigfile = file;
        try {
            bigfile = bigfile.getCanonicalFile();
        } catch (IOException ignored) { }

        String name = bigfile.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) name = name.substring(0, dot);
        setTitle(name);
    }

    public void printSchedule() {
        scheduleViewer.printSchedule();
    }

    private class PrintScheduleAction extends AbstractAction {
        public PrintScheduleAction() {
            putValue(NAME, "Print...");
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }

        public void actionPerformed(ActionEvent e) {
            printSchedule();
        }
    }
}
