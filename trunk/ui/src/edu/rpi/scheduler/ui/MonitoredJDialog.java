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

import javax.swing.JDialog;
import java.awt.HeadlessException;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Dialog;

public class MonitoredJDialog extends JDialog {
    {
        ExitManager.windowStateUpdated(this);
    }

    public MonitoredJDialog() throws HeadlessException {
    }

    public MonitoredJDialog(Frame owner) throws HeadlessException {
        super(owner);
    }

    public MonitoredJDialog(Frame owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public MonitoredJDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public MonitoredJDialog(Frame owner, String title, boolean modal)
            throws HeadlessException {
        super(owner, title, modal);
    }

    public MonitoredJDialog(Frame owner, String title, boolean modal,
            GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public MonitoredJDialog(Dialog owner) throws HeadlessException {
        super(owner);
    }

    public MonitoredJDialog(Dialog owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public MonitoredJDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public MonitoredJDialog(Dialog owner, String title, boolean modal)
            throws HeadlessException {
        super(owner, title, modal);
    }

    public MonitoredJDialog(Dialog owner, String title, boolean modal,
            GraphicsConfiguration gc) throws HeadlessException {
        super(owner, title, modal, gc);
    }
}
