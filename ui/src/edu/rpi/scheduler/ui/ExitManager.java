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

import java.awt.Window;
import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;

public final class ExitManager {
    private static final Logger logger = Logger.getLogger(ExitManager.class.getName());

    private static long lastUpdate = 0;
    private static final WindowStateListener stateListener = new WindowStateListener() {
                public void windowStateChanged(WindowEvent e) {
                    windowStateUpdated(e.getWindow());
                }
            };
    private static Set<Window> added = new HashSet<Window>();
    private static final WindowListener windowListener = new WindowListener() {
        public void windowOpened(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowClosing(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowClosed(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowIconified(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowDeiconified(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowActivated(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }

        public void windowDeactivated(WindowEvent e) {
            windowStateUpdated(e.getWindow());
        }
    };

    private static long waitUntil = 0;

    public static void start() {
        Timer timer = new Timer("Exit Manager", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                exitIfNecessary();
            }
        }, 0, 1000);
    }

    private ExitManager() { }

    private static boolean everOpened = false;
    private static Set<Window> openWindows = new HashSet<Window>();

    public static synchronized void windowStateUpdated(Window window) {
        if (added.add(window)) {
            window.addWindowStateListener(stateListener);
            window.addWindowListener(windowListener);
            // allow for slow computers
            waitUntil = System.currentTimeMillis() + 10000;
        }
        if (window.isShowing()) openWindows.add(window);
        else openWindows.remove(window);

        everOpened = true;
        lastUpdate = System.currentTimeMillis();
    }

    private static synchronized void exitIfNecessary() {
        if (!everOpened) return;
        long time = System.currentTimeMillis();
        if (time - lastUpdate < 5000) return;
        if (time < waitUntil) return;
        if (openWindows.isEmpty()) {
            logger.log(Level.INFO, "Exiting because all windows are closed");
            System.exit(0);
        }
    }
}
