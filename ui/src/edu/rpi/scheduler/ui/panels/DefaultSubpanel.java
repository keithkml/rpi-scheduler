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

package edu.rpi.scheduler.ui.panels;

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.spec.SchedulerData;
import edu.rpi.scheduler.ui.SchedulerPanel;
import edu.rpi.scheduler.ui.SchedulingSession;

import javax.swing.JOptionPane;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Provides a slightly simplified interface for creating a subpanel.
 */
public abstract class DefaultSubpanel extends Subpanel {
    private static final Logger logger
            = Logger.getLogger(DefaultSubpanel.class.getName());

    private SchedulingSession session;
    private SchedulerPanel schedulerPanel;

    public void setSession(SchedulingSession session) {
        this.session = session;
    }

    /**
     * Stores the given scheduler panel, so you can retrieve it using {@link
     * #getSchedulerPanel}.
     * @param panel the parent scheduler panel of this subpanel
     */
    public void setSchedulerPanel(SchedulerPanel panel) {
        this.schedulerPanel = panel;
    }

    public final SchedulerData getSchedulerData() {
        return session.getEngine().getSchedulerData();
    }

    public final SchedulerEngine getEngine() {
        return session.getEngine();
    }

    public SchedulingSession getSession() {
        return session;
    }

    /**
     * Returns the parent {@code SchedulerPanel} of this panel.
     * @return the parent {@code SchedulerPanel} of this panel
     */
    public final SchedulerPanel getSchedulerPanel() { return schedulerPanel; }

    public boolean canProgress() {
        return true;
    }

    /**
     * Does nothing.
     */
    public void init() { }

    /**
     * Returns {@code true}.
     * @return {@code true}
     */
    public boolean preEnter() {
        return true;
    }

    /**
     * Does nothing.
     */
    public void entering() { }

    /**
     * Does nothing.
     */
    public void progressing() { }

    public String getNextButtonText() {
        return "Next Page";
    }

    public String getPrevButtonText() {
        return "Previous Page";
    }

    public void disposeSubpanel() {
    }

    public void handleEnteringError(Throwable t) {
        StringWriter caw = new StringWriter();
//        caw.write(t.getMessage() + "\n");
        t.printStackTrace(new PrintWriter(caw));
        String email = getSession().getUIPlugin().getAdminEmail();
        logger.log(Level.SEVERE, "Error entering " + getClass().getName(), t);
        JOptionPane.showMessageDialog(getSchedulerPanel(),
                "I'm sorry, but a big error occurred deep inside the scheduler. \nYou "
                + "should quit the scheduler and reopen it. If you want, you\n "
                + "can send an email to " + email + " with this information:\n\n"
                + caw.toString(),
                "Scheduler Error", JOptionPane.ERROR_MESSAGE);
    }

    public String getInfoBarText() { return null; }
}
