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

import net.java.plaf.LookAndFeelPatchManager;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.BasicService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.UIManager;
import java.awt.Toolkit;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;

public class Scheduler {
    //TOMAYBE: show conflicts in separate ox
    //TOMAYBE: check for updates
    private static final Logger logger = Logger.getLogger(Scheduler.class.getName());

    public static void main(final String[] args) {
        initLogging();

        initStaticUIFeatures();

        String codebaseProp = System.getProperty("scheduler.codebase");
        URL codebase = null;
        try {
            if (codebaseProp != null) codebase = new URL(codebaseProp);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Invalid codebase URL provided in "
                    + "scheduler.codebase property: " + codebaseProp, e);
        }
        try {
            BasicService basic = (BasicService)
                    ServiceManager.lookup(BasicService.class.getName());
            codebase = basic.getCodeBase();
        } catch (UnavailableServiceException e) {
            logger.log(Level.SEVERE, "Couldn't load BasicService to determine "
                    + "document root", e);
        }
        final SchedulerInitializer initializer = new SchedulerInitializer();
        initializer.addInitializerListener(new SchedulerInitializerListener() {
            public void loadedInitialThings(SchedulerInitializer initializer) {
                initializer.loadMainUI(args);
            }
        });

        loadSingleInstanceService(initializer);

        if (codebase != null) initializer.setCodebase(codebase);

        initializer.init();
        initializer.loadInitialThings();

        ExitManager.start();
    }

    private static void initLogging() {
        Logger schedlogger = Logger.getLogger("edu.rpi.scheduler");
        schedlogger.setLevel(Level.ALL);
        File logsDir = new File(UITools.getSchedulerConfigFolder(), "logs");
        logsDir.mkdir();
        File logFile = new File(logsDir, "scheduler-"
                + new Date().toString().replaceAll("[^A-Za-z0-9.-]+", "-")
                + ".log");
        FileHandler handler = null;
        try {
            handler = new FileHandler(logFile.getAbsolutePath(), false);
        } catch (IOException e) {
            System.err.println("Couldn't open log file: "
                    + logFile.getAbsolutePath());
            e.printStackTrace();
        }
        handler.setFormatter(new MyXMLFormatter());
        if (handler != null) {
            schedlogger.addHandler(handler);
            // delete old log files
            for (File file : logsDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".log");
                }
            })) {
                long age = System.currentTimeMillis() - file.lastModified();
                if (age > 7*86400*1000) file.delete();
            }
        }
    }

    private static void loadSingleInstanceService(
            final SchedulerInitializer initializer) {
        try {
            final SingleInstanceService service
                    = (SingleInstanceService) ServiceManager.lookup(
                            SingleInstanceService.class.getName());
            final SingleInstanceListener listener
                    = new SchedulerInstanceListener(initializer);
            Thread hook = new Thread() {
                public void run() {
                    service.removeSingleInstanceListener(listener);
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);
            service.addSingleInstanceListener(listener);
        } catch (Exception ignored) {
            logger.log(Level.WARNING, "Error while setting up single instance "
                    + "initializer");
        }
    }

    private static void initStaticUIFeatures() {
        if (System.getProperty("swing.defaultlaf") == null) {
            try {
                // make the program look native
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Couldn't set native L&F", e);
            }
        }
        try {
            Class<?> cls = Class.forName("com.jgoodies.clearlook.ClearLookManager");
            Method method = cls.getMethod("installDefaultMode", new Class[0]);
            method.invoke(null, new Object[0]);
        } catch (Throwable e) {
            logger.log(Level.FINE, "Couldn't initialize ClearLook", e);
        }

        try {
            LookAndFeelPatchManager.initialize();
        } catch (Throwable e) {
            logger.log(Level.FINE, "Couldn't initialize WinLAF enhancements",
                    e);
        }

//        try {
//            DownloadService downloads = (DownloadService)
//                    ServiceManager.lookup(DownloadService.class.getName());
//        } catch (UnavailableServiceException e) {
//            e.printStackTrace();
//        }

        Toolkit.getDefaultToolkit().setDynamicLayout(true);
    }

    private static class SchedulerInstanceListener implements SingleInstanceListener {
        private final SchedulerInitializer initializer;

        public SchedulerInstanceListener(SchedulerInitializer initializer) {
            this.initializer = initializer;
        }

        public void newActivation(String[] strings) {
            initializer.waitForInitialThingsToLoad();
            initializer.loadMainUI(strings);
        }
    }
}
