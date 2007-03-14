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

package edu.rpi.scheduler.ui;

import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.ui.panels.Subpanel;
import edu.rpi.scheduler.ui.panels.courses.CourseSelectionPanel;
import edu.rpi.scheduler.ui.panels.narrow.NarrowDownPanel;
import edu.rpi.scheduler.ui.panels.view.ViewSchedulesPanel;
import edu.rpi.scheduler.ui.print.SchedulePrinter;
import edu.rpi.scheduler.ui.savesched.LoadedSchedule;
import edu.rpi.scheduler.ui.savesched.LoadedScheduleWindow;
import edu.rpi.scheduler.ui.savesched.SchedulePersister;
import edu.rpi.scheduler.ui.widgets.ScheduleGrid;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class SchedulerWindow extends MonitoredJFrame {
    private static final Logger logger
            = Logger.getLogger(SchedulerWindow.class.getName());

    private static final Preferences PREFERENCES
            = Preferences.userNodeForPackage(SchedulerWindow.class);
    private static final String KEY_MAIN_WINDOW = "main-window";
    private static final FileFilter SCHEDULE_FILE_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory()
                    || f.getName().toLowerCase().endsWith(".schedule");
        }

        public String getDescription() {
            return "Saved schedules";
        }
    };

    private SchedulingSession session;

    private SchedulerPanel schedulerPanel = null;

    private BackgroundWorker bgWorker = null;

    private JMenuBar menuBar;
    private JFileChooser fileOpenBox;

//    private NewSessionAction newSessionAction = new NewSessionAction();
    private OpenScheduleAction openScheduleAction = new OpenScheduleAction();
    private SaveScheduleAction saveScheduleAction = new SaveScheduleAction();
    private PrintScheduleAction printScheduleAction = new PrintScheduleAction();
    private CloseWindowAction closeWindowAction = new CloseWindowAction();
    private boolean closed = false;

    public SchedulerWindow(SchedulingSession session) {
        this.session = session;
    }

    public void init() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                closeWindow();
            }
        });

        UITools.loadWindowPosition(this, PREFERENCES, KEY_MAIN_WINDOW);
        if (!UITools.loadWindowSize(this, PREFERENCES, KEY_MAIN_WINDOW)) {
            setSize(session.getUIPlugin().getDefaultWindowSize(WindowType.MAIN));
        }

        schedulerPanel = new SchedulerPanel(session,
                SchedulerWindow.this.bgWorker);

        SchedulerUIPlugin plugin = session.getUIPlugin();
        setTitle(plugin.getProgramName());
        Image icon = plugin.getWindowIcon(WindowType.MAIN);
        if (icon != null) setIconImage(icon);
        initializeMenus();
        getContentPane().add(schedulerPanel);
        Subpanel newcsp = new CourseSelectionPanel();
        schedulerPanel.addSubpanel(newcsp);
        newcsp.init();
        BackgroundWorker bgWorker = new BackgroundWorker();
        this.bgWorker = bgWorker;

        bgWorker.workOn(new Runnable() {
            public void run() {
                final NarrowDownPanel newtsp = new NarrowDownPanel();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        schedulerPanel.addSubpanel(newtsp);
                        newtsp.init();
                    }
                });
            }
        });
        bgWorker.workOn(new Runnable() {
            public void run() {
                final ViewSchedulesPanel newvsp = new ViewSchedulesPanel();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        schedulerPanel.addSubpanel(newvsp);
                        newvsp.init();
                    }
                });
            }
        });
        bgWorker.start();
    }

    private void initializeMenus() {
        // Set up menus
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                if (schedulerPanel == null) return;
                Subpanel subpanel = schedulerPanel.getCurrentSubpanel();
                boolean canShowSchedule
                        = subpanel instanceof SchedulePresenter;
                boolean showingSchedule = false;
                if (canShowSchedule) {
                    SchedulePresenter presenter = (SchedulePresenter) subpanel;
                    ScheduleInfo schedInfo = presenter.getCurrentSchedule();
                    showingSchedule = schedInfo.getSchedule() != null;
                }
                saveScheduleAction.setEnabled(showingSchedule);
                printScheduleAction.setEnabled(showingSchedule);
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });
        menuBar.add(fileMenu);

        //TOSMALL: new session
//        fileMenu.add(newSessionAction);

        fileMenu.add(openScheduleAction);

        fileMenu.add(saveScheduleAction);

        fileMenu.addSeparator();

        fileMenu.add(printScheduleAction);

        fileMenu.addSeparator();

        fileMenu.add(closeWindowAction);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        menuBar.add(helpMenu);
        for (Link link : session.getUIPlugin().getLinks()) {
            helpMenu.add(new URLAction(link.getName(), link.getUrl()));
        }
        helpMenu.add(new AboutAction());
//        helpMenu.add(new AbstractAction() {
//            {
//                putValue(NAME, "Threads");
//            }
//            public void actionPerformed(ActionEvent e) {
//                Timer timer = new Timer("Thread shower", true);
//                timer.schedule(new TimerTask() {
//                    public void run() {
//                        showThreads();
//                    }
//                }, 10000);
//            }
//        });

        fileOpenBox = new JFileChooser();
        fileOpenBox.setDragEnabled(true);
        fileOpenBox.addChoosableFileFilter(SCHEDULE_FILE_FILTER);
    }

    private void showThreads() {
        StringBuffer buf = new StringBuffer(100);
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] els = entry.getValue();

            if (thread.isAlive() && !thread.isDaemon()) {
                StackTraceElement el = null;
                if (els.length > 0) el = els[0];
                buf.append(thread.getName() + ": " + el + "\n");
            }
        }
        JOptionPane.showMessageDialog(null, buf.toString());
    }

    private void setError(String error, Exception e) {
        logger.log(Level.SEVERE, "Error during startup: " + error, e);

        String msg = "<HTML><B>" + error + "</B><BR><BR>"
                + "Please try again later.";
        JOptionPane.showMessageDialog(null,
                msg, "Scheduler error", JOptionPane.ERROR_MESSAGE);
    }

    public void closeWindow() {
        if (closed) return;
        closed = true;
        setVisible(false);
        dispose();
        UITools.saveWindowPositionAndSize(SchedulerWindow.this,
                PREFERENCES, KEY_MAIN_WINDOW);
        if (schedulerPanel != null) schedulerPanel.disposeSubpanels();
    }

    public void openScheduleFile(File file) {
        LoadedSchedule scheduleInfo = UITools.loadSchedule(this, session, file);
        if (scheduleInfo == null) return;

        LoadedScheduleWindow lsw = new LoadedScheduleWindow(session);
        lsw.setSchedule(this, scheduleInfo, file);
        lsw.setVisible(true);
    }

    private class NewSessionAction extends AbstractAction {
        public NewSessionAction() {
            super("New Session");

            putValue(MNEMONIC_KEY, KeyEvent.VK_N);
        }

        public void actionPerformed(ActionEvent e) {

        }
    }

    private class OpenScheduleAction extends AbstractAction {
        public OpenScheduleAction() {
            super("Open Schedule...");

            putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        }

        public void actionPerformed(ActionEvent e) {
            fileOpenBox.setDialogTitle("Open Schedule");
            int result = fileOpenBox.showOpenDialog(SchedulerWindow.this);
            if (result != JFileChooser.APPROVE_OPTION) return;
            File file = fileOpenBox.getSelectedFile();

            openScheduleFile(file);
        }
    }

    private class SaveScheduleAction extends AbstractAction {
        public SaveScheduleAction() {
            super("Save Schedule...");

            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
        }

        public void actionPerformed(ActionEvent e) {
            fileOpenBox.setDialogTitle("Save Schedule");
            int result = fileOpenBox.showSaveDialog(SchedulerWindow.this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File file = fileOpenBox.getSelectedFile();
            if (file == null) return;

            if (fileOpenBox.getFileFilter() == SCHEDULE_FILE_FILTER) {
                if (!file.getName().toLowerCase().endsWith(".schedule")) {
                    file = new File(file.getPath() + ".schedule");
                }
            }

            SchedulePersister sp = new SchedulePersister();
            Subpanel panel = schedulerPanel.getCurrentSubpanel();
            if (!(panel instanceof SchedulePresenter)) {
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "I could not save the "
                        + "current schedule because it appears to no longer be "
                        + "on the screen.", "Save Schedule Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            SchedulePresenter presenter = (SchedulePresenter) panel;
            Schedule schedule = presenter.getCurrentSchedule().getSchedule();
            if (schedule == null) {
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "I could not save the "
                        + "current schedule because I don't see a schedule on "
                        + "your screen.", "Save Schedule Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                FileOutputStream stream = new FileOutputStream(file);
                sp.saveSchedule(session, schedule, stream);
                stream.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Couldn't save schedule", ex);
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "I could not save the "
                        + "current schedule because of an error saving to the "
                        + "file you selected.", "Save Schedule Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    private class PrintScheduleAction extends AbstractAction {
        public PrintScheduleAction() {
            super("Print Schedule...");

            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }

        public void actionPerformed(ActionEvent e) {
            Subpanel panel = schedulerPanel.getCurrentSubpanel();
            if (!(panel instanceof SchedulePresenter)) {
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "I could not print the "
                        + "current schedule because it appears to no longer be "
                        + "on the screen.", "Save Schedule Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            SchedulePresenter presenter = (SchedulePresenter) panel;
            Schedule schedule = presenter.getCurrentSchedule().getSchedule();
            if (schedule == null) {
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "I could not print the "
                        + "current schedule because I don't see a schedule on "
                        + "your screen.", "Save Schedule Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            SchedulePrinter printer = new SchedulePrinter(session, schedule);
            ScheduleGrid grid = presenter.getScheduleGrid();
            if (grid != null) printer.setOriginalGrid(grid);
            UITools.printSchedule(SchedulerWindow.this, printer);
        }
    }

    private class CloseWindowAction extends AbstractAction {
        public CloseWindowAction() {
            super("Close");

            putValue(MNEMONIC_KEY, KeyEvent.VK_C);
        }

        public void actionPerformed(ActionEvent e) {
            closeWindow();
        }
    }

    private class URLAction extends AbstractAction {
        private URL url;

        public URLAction(String name, String urlstr) {
            try {
                url = new URL(urlstr);
            } catch (MalformedURLException e) {
                setEnabled(false);
                return;
            }
            putValue(NAME, name);
        }


        public void actionPerformed(ActionEvent e) {
            try {
                BasicService bs = (BasicService) ServiceManager.lookup(BasicService.class.getName());
                bs.showDocument(url);
            } catch (UnavailableServiceException e1) {
                JOptionPane.showMessageDialog(SchedulerWindow.this,
                        "Sorry, for some reason the web page could not be "
                        + "loaded. Try opening " + url.toExternalForm()
                        + " in your web browser manually.");
                return;
            }
        }
    }

    private class AboutAction extends AbstractAction {
        public AboutAction() {
            putValue(NAME, "About");
        }

        public void actionPerformed(ActionEvent e) {
            SchedulerUIPlugin plugin = session.getUIPlugin();
            JTextPane text = new JTextPane();
            text.setContentType("text/html");
            text.setText(plugin.getAboutBoxText());
            UITools.makeTextPaneLookLikeDialog(text);
            JScrollPane pane = new JScrollPane(text);
            pane.setBorder(new EmptyBorder(0,0,0,0));
            pane.setPreferredSize(new Dimension(300, 150));
            JOptionPane.showMessageDialog(SchedulerWindow.this,
                    pane, "About " + plugin.getProgramName(),
                    JOptionPane.PLAIN_MESSAGE);
        }
    }
}
