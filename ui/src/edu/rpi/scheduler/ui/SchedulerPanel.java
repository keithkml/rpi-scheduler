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

import edu.rpi.scheduler.ui.panels.Subpanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SchedulerPanel extends JPanel {
    // fix layout problems

    private JButton prevButton;
    private JButton nextButton;
    private JPanel mainPanel;
    private JLabel panelTitle;
    private JPanel subpanelHolder;
    private JTextPane infoBar;

    private List<Subpanel> panels = new ArrayList<Subpanel>();
    private int currentPanelIndex = -1;

    private NextAction nextAction;
    private PreviousAction prevAction;

    private BackgroundWorker bgWorker;

    private SchedulingSession session;
    private JScrollPane infoBarBox;
    private JPanel bottomPanel;

    public SchedulerPanel(SchedulingSession session, BackgroundWorker worker) {
        this.session = session;
        this.bgWorker = worker;

        init();
    }

    private synchronized void init() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                disposeSubpanels();
            }
        });

        prevAction = new PreviousAction();
        prevButton.setAction(prevAction);
        nextAction = new NextAction();
        nextButton.setAction(nextAction);

        setLayout(new BorderLayout());
        add(mainPanel);
        subpanelHolder.setLayout(new BorderLayout());

        UITools.makeTextPaneLookLikeDialog(infoBar);
        infoBarBox.setBorder(new EmptyBorder(0, 0, 0, 0));

        panelTitle.setFont(panelTitle.getFont().deriveFont(Font.BOLD, 18));
    }


    public synchronized void addSubpanel(final Subpanel panel) {
        panel.setSession(session);
        panel.setSchedulerPanel(this);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panels.add(panel);

                if (currentPanelIndex == -1) showPanel(0);
                if (currentPanelIndex == panels.size() - 2) updateButtonText();
            }
        });
    }

    public synchronized boolean showPanel(int index) {
        Subpanel current = getCurrentPanel();

        if (current != null && index > currentPanelIndex && !current.canProgress()) {
            return false;
        }

        Subpanel next = getPanel(index);

        if (current != null) current.progressing();

        try {
            next.init();
            if (!next.preEnter()) return false;
            next.entering();
        } catch (Throwable t) {
            next.handleEnteringError(t);
            return false;
        }

        if (current != null) remove(current);

        subpanelHolder.removeAll();
        subpanelHolder.add(next);

        currentPanelIndex = index;

        infoBar.setEnabled(true);
        panelTitle.setEnabled(true);

        updateButtonText();
        updatePrevStatus();
        updateNextStatus();

        SwingUtilities.getAncestorOfClass(Window.class, this).validate();
//        repaint();

        return true;
    }

    private synchronized Subpanel getCurrentPanel() {
        return currentPanelIndex == -1 ? null : getPanel(currentPanelIndex);
    }

    private synchronized Subpanel getPanel(int index) {
        if (index < 0 || index >= panels.size()) return null;

        return panels.get(index);
    }

    private void updateButtonText() {
        Subpanel prev = getPanel(currentPanelIndex - 1);
        Subpanel next = getPanel(currentPanelIndex + 1);

        String prevText;
        String nextText;
//        prevText = prev == null ? null : prev.getPrevButtonText();
//        nextText = next == null ? null : next.getNextButtonText();

//        if (prevText == null) prevText = "Previous";
//        if (nextText == null) nextText = "Next";
        prevText = "Previous";
        nextText = "Next";

        prevButton.setText("< " + prevText);
        nextButton.setText(nextText + " >");
    }

    private void updatePrevStatus() {
        prevButton.setEnabled(currentPanelIndex > 0);
    }

    public void updateNextStatus() {
        Subpanel panel = getCurrentPanel();
        if (panel == null) {
            infoBarBox.setVisible(false);
            bottomPanel.validate();
            nextButton.setEnabled(false);
            return;
        }

        boolean enabled;
        if (getPanel(currentPanelIndex + 1) == null) enabled = false;
        else enabled = panel.canProgress();

        nextButton.setEnabled(enabled);

        String title = panel.getPanelTitle();
        if (title == null) {
            panelTitle.setVisible(false);
        } else {
            panelTitle.setVisible(true);
            panelTitle.setText(title);
        }

        String infoText = panel.getInfoBarText();
        if (infoText == null) {
            infoBarBox.setVisible(false);
        } else {
            infoBarBox.setVisible(true);
            infoBar.setText(infoText);
        }
        validate();
    }

    public BackgroundWorker getBGWorker() {
        return bgWorker;
    }

    public Subpanel getCurrentSubpanel() {
        return getPanel(currentPanelIndex);
    }

    public void disposeSubpanels() {
        for (Subpanel subpanel : panels) {
            subpanel.disposeSubpanel();
        }
        removeAll();
        panels.clear();
    }

    public class PreviousAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            showPanel(currentPanelIndex - 1);
        }
    }

    public class NextAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            showPanel(currentPanelIndex + 1);
        }
    }

}
