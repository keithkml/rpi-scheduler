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

import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.SECTION;
import static edu.rpi.scheduler.ui.WindowType.LOADING_PROGRESS;
import edu.rpi.scheduler.CopyOnWriteArrayList;
import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.DefaultXmlDataPlugin;
import edu.rpi.scheduler.schedb.load.DbLoadException;
import edu.rpi.scheduler.schedb.load.spec.DataLoadingContext;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoadListener;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoader;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.ResourceLoader;
import edu.rpi.scheduler.schedb.spec.SchedulerData;
import edu.rpi.scheduler.ui.savesched.LoadedSchedule;
import edu.rpi.scheduler.ui.savesched.LoadedScheduleWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Image;

public class SchedulerInitializer {
    private static final Logger logger
            = Logger.getLogger(SchedulerInitializer.class.getName());

    public static final String SYSPROP_DBURL = "scheduler.dburl";
    public static final String SYSPROP_DBPLUGINNAME = "scheduler.dbplugin";
    public static final String SYSPROP_UIPLUGINNAME = "scheduler.uiplugin";

    private LoadingProgressWindow progressWindow = new LoadingProgressWindow();
    private BackgroundWorker worker = new BackgroundWorker();
    private CopyOnWriteArrayList<SchedulerInitializerListener> listeners
            = new CopyOnWriteArrayList<SchedulerInitializerListener>();
    private SchedulingSession session = new SchedulingSession();
    private SchedulerWindow schedulerWindow;
    private boolean loadedInitialThings = false;
    private final Object initialThingsLock = new Object();

    public void init() {
        progressWindow.setMessage("Loading scheduler...");
        progressWindow.pack();
        progressWindow.setLocationRelativeTo(null);
        progressWindow.setVisible(true);

        worker.start();
    }

    public void addInitializerListener(SchedulerInitializerListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeInitializerListener(SchedulerInitializerListener listener) {
        listeners.remove(listener);
    }

    public void loadInitialThings() {
        if (loadDatabaseURL() && loadDbPlugin()) {
            if (loadUIPlugin()) {
                //TOSMALL: load stuff while loading db
                loadUISettings();
                SchedulerUIPlugin uiPlugin = session.getUIPlugin();
                Image icon = uiPlugin.getWindowIcon(LOADING_PROGRESS);
                progressWindow.setIconImage(icon);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressWindow.setHasMessage(false);
                        progressWindow.setMessage("Loading course database...");
                    }
                });
                loadDatabase();
            } else {
                return;
            }
        } else {
            return;
        }
    }

    private boolean argsAreImportant(String[] args) {
        if (args.length == 2) {
            String cmd = args[0];
            return cmd.equals("-open") || cmd.equals("-print");
        } else {
            return false;
        }
    }

    private void loadUISettings() {
        SchedulerUIPlugin plugin = session.getUIPlugin();
        try {
            plugin.loadSettings();
        } catch (PluginLoadingException e) {
            showError("The Scheduler cannot be loaded because your "
                    + "database administrators have misconfigured it.",
                    e);
        }
    }

    private void processArgs(String[] args) {
        if (args.length == 2) {
            final String fname = args[1];
            String cmd = args[0];
            if (cmd.equals("-open")) {
                openSchedule(fname);
            } else if (cmd.equals("-print")) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        LoadedScheduleWindow lsw = openSchedule(fname);
                        lsw.printSchedule();
                    }
                });
            }
        }
    }

    private LoadedScheduleWindow openSchedule(final String fname) {
        final LoadedScheduleWindow lsw = new LoadedScheduleWindow(session);
        File file = new File(fname);
        LoadedSchedule scheduleInfo = UITools.loadSchedule(null,
                session, file);
        lsw.setSchedule(null, scheduleInfo, file);
        if (scheduleInfo != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    lsw.setVisible(true);
                }
            });
            return lsw;
        }
        return null;
    }

    private boolean loadUIPlugin() {
        SchedulerUIPlugin uiPlugin;

        String uiPluginName = System.getProperty(SYSPROP_UIPLUGINNAME);
        if (uiPluginName == null) {
            uiPlugin = new XmlConfiguredUIPlugin(session);

        } else {
            Class pluginClass;
            try {
                pluginClass = Class.forName(uiPluginName);
            } catch (Exception e) {
                showError("Your school's scheduler administrators have "
                        + "misconfigured their UI plug-in.", e);
                return false;
            }
            try {
                uiPlugin = (SchedulerUIPlugin) pluginClass.newInstance();
            } catch (Exception e) {
                showError("Your school's scheduler administrators have "
                        + "provided a broken UI plug-in.", e);
                return false;
            }
        }
        session.setUIPlugin(uiPlugin);
        return true;
    }

    private void showError(String msg, Exception e) {
        logger.log(Level.SEVERE, msg, e);

        JOptionPane.showMessageDialog(null, msg, "Scheduler Error",
                JOptionPane.ERROR_MESSAGE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressWindow.setVisible(false);
                progressWindow.dispose();
            }
        });
    }

    private boolean loadDbPlugin() {
        SchedulerDataPlugin plugin;
        String dbPluginName = System.getProperty(SYSPROP_DBPLUGINNAME);
        if (dbPluginName == null) {
            plugin = new DefaultXmlDataPlugin();

        } else {
            Class pluginClass;
            try {
                pluginClass = Class.forName(dbPluginName);
            } catch (ClassNotFoundException e) {
                showError("Your school's scheduler administrators have "
                        + "misconfigured their database plug-in.", e);
                return false;
            }
            try {
                plugin = (SchedulerDataPlugin) pluginClass.newInstance();
            } catch (Exception e) {
                showError("Your school's scheduler administrators have "
                        + "provided a broken database plug-in.", e);
                return false;
            }
        }
        session.setDataPlugin(plugin);
        return true;
    }

    private boolean loadDatabaseURL() {
        String dbUrl = System.getProperty(SYSPROP_DBURL);
        if (dbUrl == null) {
            showError("Your school's scheduler administrators have "
                    + "misconfigured the database URL.", null);
            return false;
        }

        String url = dbUrl;
        URL codebase = session.getCodebase();
        logger.log(Level.FINE, "Codebase: " + codebase);
        if (codebase != null) {
            try {
                url = new URL(codebase, dbUrl).toExternalForm();
                logger.log(Level.FINE, "Using URL " + url);
            } catch (MalformedURLException e) {
                showError("Your school's scheduler administrators have "
                        + "misconfigured the database URL.", e);
                return false;
            }
        }
        session.setDatabaseLocation(url);
        return true;
    }

    public void loadDatabase() {
        worker.workOn(new Runnable() {
            public void run() {
                try {
                    loadDatabaseReally();
                } catch (DbLoadException e) {
                    showError("The database could not be loaded.", e);
                    return;
                }
                progressWindow.setHasMessage(true);
                progressWindow.setMessage("Loading scheduler...");

                setLoadedInitialThings();
                for (SchedulerInitializerListener listener : listeners) {
                    listener.loadedInitialThings(SchedulerInitializer.this);
                }
            }
        });
    }

    private void setLoadedInitialThings() {
        synchronized(initialThingsLock) {
            loadedInitialThings = true;
            initialThingsLock.notifyAll();
        }
    }

    public boolean loadedInitialThings() {
        synchronized(initialThingsLock) {
            return loadedInitialThings;
        }
    }

    public boolean waitForInitialThingsToLoad() {
        synchronized(initialThingsLock) {
            for (;;) {
                if (loadedInitialThings) return true;
                try {
                    initialThingsLock.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
    }

    public void loadMainUI(String[] args) {
        if (argsAreImportant(args)) {
            processArgs(args);
        } else {
            schedulerWindow = new SchedulerWindow(session);
            schedulerWindow.init();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    schedulerWindow.setVisible(true);
                }
            });
        }
        if (progressWindow.isShowing()) {
            progressWindow.setVisible(false);
            progressWindow.dispose();
        }
    }

    private void loadDatabaseReally() throws DbLoadException {
        DataContext dataContext = session.getDataContext();
        DataLoadingContext loadContext
                = dataContext.getDataLoadingContext();
        DatabaseLoader fileLoader = loadContext.getFileLoader();
        String dburl = session.getDatabaseLocation();
        ResourceLoader loader = session.getResourceLoader();
        fileLoader.setResourceLoader(loader);
        try {
            fileLoader.loadDb(dburl, new DatabaseLoadListener() {
                public void loadingDepartment(DataLoadingContext context, String name) {
                }

                public void loadingCourse(final DataLoadingContext context, final String name) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressWindow.setCurrentCourse(name);
                            int loaded = context.getLoadedObjectCount(SECTION);
                            int total = context.getTotalObjectCount(SECTION);
                            progressWindow.setProgress(loaded, total);
                        }
                    });
                }

                public void loadingSection(DataLoadingContext context, String number) {
                }
            });
        } catch (DbLoadException e) {
            logger.log(Level.WARNING, "Removing " + dburl + " from cache due to exception", e);
            loader.removeFromCache(dburl);
            throw e;
        }
        SchedulerEngine engine = session.getEngine();
        SchedulerData schedulerData = loadContext.getSchedulerDataObj();
        engine.setSchedulerData(schedulerData);
    }

    public void setCodebase(URL codebase) {
        session.setCodebase(codebase);
    }
}
