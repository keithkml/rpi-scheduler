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

package edu.rpi.scheduler.ui.panels.courses;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.StringFormatType;
import edu.rpi.scheduler.ui.widgets.MiniSchedulePanel;
import edu.rpi.scheduler.ui.widgets.SchedulePaintProvider;
import edu.rpi.scheduler.ui.widgets.SingleCourseGrid;
import edu.rpi.scheduler.ui.widgets.TimeGridSchedulePainter;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class CourseTimeViewerPanel extends JPanel {
    private static final Comparator<Section> SECNUM_COMPARATOR
            = new Comparator<Section>() {
        public int compare(Section section, Section section1) {
            return section.getNumber().compareTo(section1.getNumber());
        }
    };

    private JPanel mainPanel;
    private SingleCourseGrid courseGrid;
    private JList sectionList;
    private JLabel titleLabel;
    private JTextPane sectionInfoBox;
    private JScrollPane sectionInfoBoxPane;

    private DefaultListModel sectionListModel = new DefaultListModel();
    private CourseDescriptor course = null;
    private SchedulingSession session = null;
    private MiniSchedulePanel miniSchedulePanel;
    private SimplePaintProvider miniPaintProvider;

    {
        setLayout(new BorderLayout());
        add(mainPanel);

        sectionList.setModel(sectionListModel);
        ListSelectionModel selModel = sectionList.getSelectionModel();
        selModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                SectionDescriptor section = (SectionDescriptor) sectionList.getSelectedValue();
                setSelectedSection(section);
            }
        });

        miniPaintProvider = new SimplePaintProvider();
        miniSchedulePanel = new MiniSchedulePanel(miniPaintProvider, session);
        sectionList.setCellRenderer(new ListCellRenderer() {
            private final Border BORDER_WHITE
                    = new LineBorder(Color.WHITE, 10);
            private final Border BORDER_BLACK
                    = new LineBorder(Color.BLACK, 1);
            private final CompoundBorder BORDER_COMP
                    = new CompoundBorder(BORDER_WHITE, BORDER_BLACK);

            private final Dimension dimension = new Dimension(100, 80);

            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                miniSchedulePanel.setPreferredSize(dimension);
                miniSchedulePanel.setMaximumSize(dimension);

                Color selfg = sectionList.getSelectionForeground();
                Color selbg = sectionList.getSelectionBackground();

                if (isSelected) {
                    miniSchedulePanel.setBackground(selbg);
                    miniPaintProvider.setPaint(selfg);
                } else {
                    miniSchedulePanel.setBackground(null);
                    miniPaintProvider.setPaint(selbg);
                }

                miniSchedulePanel.setOpaque(isSelected);
                SectionDescriptor sd = (SectionDescriptor) value;
                UniqueSection us = UniqueSection.getInstance(sd);
                miniSchedulePanel.setSections(Collections.singleton(us));
                miniSchedulePanel.setBorder(BORDER_COMP);


                return miniSchedulePanel;
            }
        });
        sectionList.addPropertyChangeListener("selectionBackground",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                TimeGridSchedulePainter painter = courseGrid.getSchedulePainter();
                Color color = (Color) evt.getNewValue();
                painter.setDefaultPaint(color);
            }
        });
        TimeGridSchedulePainter painter = courseGrid.getSchedulePainter();
        painter.setDefaultPaint(sectionList.getSelectionBackground());

        UITools.makeTextPaneLookLikeDialog(sectionInfoBox);
        sectionInfoBoxPane.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
        courseGrid.setSession(session);
        miniSchedulePanel.setSession(session);
    }

    public void setCourse(CourseDescriptor cd) {
        course = cd;
        String text;
        if (cd == null) {
            text = session.getUIPlugin().getCourseString(cd, StringFormatType.COURSE_SHORT);
        } else {
            text = "";
        }
        titleLabel.setText(text);
        sectionListModel.clear();
        courseGrid.setCourse(cd);

        if (cd != null) {
            Collection<Section> unsortedSections = cd.getActualCourse().getSections();
            List<Section> sections = new ArrayList<Section>(unsortedSections);
            Collections.sort(sections, SECNUM_COMPARATOR);
            for (Section section : sections) {
                sectionListModel.addElement(new SectionDescriptor(cd, section));
            }

            // select the first section if there's only one
            if (sections.size() == 1) sectionList.setSelectedIndex(0);
            else setSelectedSection(null);
        } else {
            setSelectedSection(null);
        }
    }

    private void setSelectedSection(SectionDescriptor sd) {
        courseGrid.setSection(sd);
        if (sd == null) {
            sectionInfoBox.setText(
                    "<font style=\"font-size: small\">"
                    + "No section selected</font>");
        } else {
            String sectionInfo = "<B>" + sd.getCourse().getName() + "</B><BR>"
                    + UITools.getSectionInfoText(sd);
            sectionInfoBox.setText(sectionInfo);
        }
        sectionInfoBox.setCaretPosition(0);
    }

    private class SimplePaintProvider implements SchedulePaintProvider {
        private Color paint = null;

        public Paint getPaint(UniqueSection section) { return paint; }

        public void setPaint(Color paint) {
            this.paint = paint;
        }
    }
}