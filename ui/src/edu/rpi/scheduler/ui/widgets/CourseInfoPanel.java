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

package edu.rpi.scheduler.ui.widgets;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.panels.courses.ConflictDetector;
import edu.rpi.scheduler.ui.panels.courses.ConflictListener;
import edu.rpi.scheduler.ui.panels.courses.SelectedCoursesList;
import edu.rpi.scheduler.ui.panels.courses.SelectedCoursesListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collection;

public class CourseInfoPanel extends JPanel {
    private CourseDescriptor course = null;

    private final JTextPane textpane = new JTextPane();

    private ConflictDetector conflictDetector = null;
    private SchedulingSession session = null;
    private SelectedCoursesList selectedCourses = null;

    {
        textpane.setContentType("text/html");
        textpane.setEditable(false);
        textpane.setOpaque(false);
        UITools.makeTextPaneLookLikeDialog(textpane);
        JScrollPane textScrollPane = new JScrollPane(textpane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        textScrollPane.setBorder(new EmptyBorder(0,0,0,0));
        add(textScrollPane, gbc);

        setCourse(null);
    }

    public void setConflictDetector(ConflictDetector conflictDetector) {
        this.conflictDetector = conflictDetector;

        conflictDetector.addListener(new ConflictListener() {
            public void conflictsUpdated(ConflictDetector detector) {
                updateTextFromOtherThread();
            }
        });
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
    }

    public SelectedCoursesList getSelectedCoursesList() {
        return selectedCourses;
    }

    public void setSelectedCoursesList(SelectedCoursesList selectedCourses) {
        this.selectedCourses = selectedCourses;
        selectedCourses.addSelectedCoursesListener(new SelectedCoursesListener() {
            public void coursesAdded(SelectedCoursesList courseList,
                    Collection<SelectedCoursesList.SelectedCourseHolder> added) {
                updateTextForCourses(added);
            }

            public void coursesRemoved(SelectedCoursesList courseList,
                    Collection<SelectedCoursesList.SelectedCourseHolder> removed) {
                updateTextForCourses(removed);
            }

            public void coursesChanged(SelectedCoursesList courseList) {
                updateText();
            }

            public void courseStatusChanged(SelectedCoursesList courseList,
                    SelectedCoursesList.SelectedCourseHolder course, boolean newExtra) {
                if (course.getCourse().equals(CourseInfoPanel.this.course)) {
                    updateText();
                }
            }
        });
    }

    private void updateTextForCourses(
            Collection<SelectedCoursesList.SelectedCourseHolder> added) {
        for (SelectedCoursesList.SelectedCourseHolder holder : added) {
            if (holder.getCourse().equals(course)) {
                updateText();
                break;
            }
        }
    }

    public CourseDescriptor getCourse() { return course; }

    public void setCourse(CourseDescriptor course) {
        this.course = course;
        updateTextFromOtherThread();
    }

    private void updateTextFromOtherThread() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateText();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateText();
                }
            });
        }
    }

    private void updateText() {
        if (session == null) return;
        SchedulerUIPlugin uiPlugin = session.getUIPlugin();
        if (uiPlugin == null) return;
        String infoText = uiPlugin.getInfoText(course, selectedCourses, conflictDetector);
        textpane.setText(infoText);
        textpane.setCaretPosition(0);
    }
}
