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

package edu.rpi.scheduler.ui.panels.view;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.Duration;
import edu.rpi.scheduler.schedb.IntRange;
import edu.rpi.scheduler.schedb.SchedulerTools;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.SectionID;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.StringFormatType;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.print.SchedulePrinter;
import edu.rpi.scheduler.ui.widgets.DetailedScheduleGrid;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class ScheduleViewer extends JPanel {
    private JPanel mainPanel;
    private JList legend;
    private DetailedScheduleGrid grid;
    private JTextPane sectionDetails;

    private JLabel classTimeLabel;
    private JLabel shortestBreakLabel;
    private JLabel creditsLabel;
    private JLabel classTimeNameLabel;
    private JLabel shortestBreakNameLabel;
    private JLabel creditsNameLabel;
    private JScrollPane legendPane;
    private JScrollPane sectionDetailsPane;

    private final Component[] scheduleDetailsComponents = {
        classTimeNameLabel, shortestBreakNameLabel, creditsNameLabel,
        classTimeLabel, shortestBreakLabel, creditsLabel,
    };

    private DefaultListModel legendList = new DefaultListModel();

    private SchedulingSession session = null;
    private Schedule schedule = null;
    
    public void init() {
        setLayout(new BorderLayout());
        add(mainPanel);

        legendPane.setBorder(new EmptyBorder(0,0,0,0));
        legend.setModel(legendList);
        legend.setCellRenderer(new DefaultListCellRenderer() {
            private LegendIcon icon = new LegendIcon();

            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                UniqueSection section = (UniqueSection) value;
                icon.setPaint(grid.getPaint(section));
                setIcon(icon);
                setText(section.getCourse().getName());
                return this;
            }
        });
        ListSelectionModel legendSel = legend.getSelectionModel();
        legendSel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                UniqueSection us = (UniqueSection) legend.getSelectedValue();
                setSelectedSection(us);
            }
        });
        grid.addSelectedSectionListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Object nv = evt.getNewValue();
                if (nv == null) {
                    legend.getSelectionModel().clearSelection();
                } else {
                    legend.setSelectedValue(nv, true);
                }
            }
        });

        UITools.makeTextPaneLookLikeDialog(sectionDetails);
        sectionDetailsPane.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
        grid.setSession(session);
    }

    public void setSchedule(Schedule sched) {
        this.schedule = sched;
        grid.setSchedule(sched);

        updateDetails();
        updateLegend();
    }

    public void setSelectedSection(UniqueSection section) {
        grid.setSelectedSection(section);
        if (section == null) {
            sectionDetails.setText(
                    "<font style=\"font-size: small\">"
                    + "No course selected</font>");
            sectionDetails.setEnabled(false);
            return;
        }
        StringBuffer buf = new StringBuffer(100);
        CourseDescriptor cd = section.getCourseDescriptor();
        buf.append("<B>" + cd.getActualCourse().getName() + "</B>");
        Collection<SectionDescriptor> sds = section.getSectionDescriptors();
        List<SectionDescriptor> sorted = new ArrayList<SectionDescriptor>(sds);
        Collections.sort(sorted);
        if (sorted.size() > 1) {
            buf.append("<HR>");
            buf.append("<FONT STYLE=\"font-size: small;\">Sections ");
            UITools.getSectionNumberList(sorted, buf);
            buf.append(" share exactly the same weekly class time, so they are "
                    + (sorted.size() == 2 ? "both" : "all") + " shown on this "
                    + "schedule.</FONT><BR><BR>");
        } else {
            buf.append("<BR>");
        }
        boolean first = true;
        for (SectionDescriptor sd : sorted) {
            if (first) first = false;
            else buf.append("<BR>");
            buf.append(UITools.getSectionInfoText(sd));
        }
        sectionDetails.setText(buf.toString());
        sectionDetails.setCaretPosition(0);
        sectionDetails.setEnabled(true);
    }

    private void updateLegend() {
        if (schedule == null) {
            legendList.clear();
            return;
        }
        UniqueSection oldSelSection = (UniqueSection) legend.getSelectedValue();
        Course oldSelCourse;
        if (oldSelSection == null) oldSelCourse = null;
        else oldSelCourse = oldSelSection.getCourse();

        Set<UniqueSection> req = new LinkedHashSet<UniqueSection>();
        Set<UniqueSection> opt = new LinkedHashSet<UniqueSection>();
        Collection<UniqueSection> sections = schedule.getSections();
        for (UniqueSection section : sections) {
            CourseDescriptor cd = section.getCourseDescriptor();
            if (session != null && session.getEngine().isExtra(cd)) opt.add(section);
            else req.add(section);
        }

        legendList.clear();
        for (UniqueSection sec : req) legendList.addElement(sec);
        for (UniqueSection sec : opt) legendList.addElement(sec);

        if (oldSelCourse != null) {
            for (int i = 0; i < legendList.size(); i++) {
                UniqueSection us = (UniqueSection) legendList.get(i);
                if (us.getCourse().equals(oldSelCourse)) {
                    legend.setSelectedValue(us, false);
                    break;
                }
            }
        }
    }

    private void updateDetails() {
        String durationString;
        String shortestTimeString;
        String creditsText;

        Schedule schedule = this.schedule;
        if (schedule == null) {
            durationString = "";
            shortestTimeString = "";
            creditsText = "";

            for (Component comp : scheduleDetailsComponents) {
                comp.setEnabled(false);
            }

        } else {
            int timeSum = 0;
            for (UniqueSection us : schedule.getSections()) {
                SectionDescriptor sd = us.getSectionDescriptors().iterator().next();
                Section section = sd.getActualSection();

                Collection<ClassPeriod> periods = section.getPeriods();
                timeSum += UITools.getTotalMinutes(periods);
            }


            durationString = new Duration(timeSum).toString();

            shortestTimeString = getShortestBreakString(schedule);

            int minCredits = 0;
            int maxCredits = 0;
            for (UniqueSection us : schedule.getSections()) {
                IntRange credits = us.getCourse().getCredits();
                if (credits == null) continue;
                minCredits += credits.getFrom();
                maxCredits += credits.getTo();
            }
            if (maxCredits > 0) {
                creditsText = minCredits
                        + (maxCredits != minCredits ? "-" + maxCredits : "");
            } else {
                creditsText = "(unknown)";
            }

            for (Component comp : scheduleDetailsComponents) {
                comp.setEnabled(true);
            }
        }

        classTimeLabel.setText(durationString);
        shortestBreakLabel.setText(shortestTimeString);
        creditsLabel.setText(creditsText);
    }


    private void addSectionInfo(Schedule schedule, StringBuffer buf) {
        buf.append("<b>Sections:</b> "
                + "<UL style=\"margin-left:13px; margin-top: 0.5em; "
                + "font-size: small; margin-right: 0px;\">");
        SchedulerUIPlugin plugin = session.getUIPlugin();
        for (UniqueSection section : schedule.getSections()) {
            CourseDescriptor cd = section.getCourseDescriptor();
            buf.append("<LI>" + plugin.getCourseString(cd, StringFormatType.COURSE_VERY_SHORT) + ": ");
            boolean first = true;
            Collection<SectionDescriptor> sds = section.getSectionDescriptors();
            List<SectionDescriptor> sorted = new ArrayList<SectionDescriptor>(sds);
            Collections.sort(sorted);
            for (SectionDescriptor sd : sorted) {
                if (first) first = false;
                else buf.append(", ");

                Section actualSection = sd.getActualSection();
                buf.append(actualSection.getNumber());
                SectionID id = actualSection.getID();
                if (id != null) {
                    buf.append(" (" + id + ")");
                }
            }
            buf.append("</LI>");
        }
        buf.append("</UL>");
    }

    private String getShortestBreakString(Schedule schedule) {
        int smallestBreak = SchedulerTools.getSmallestBreak(schedule);
        String betweenTimeString;
        if (smallestBreak == -1) {
            betweenTimeString = "(none)";
        } else if (smallestBreak < 60) {
            betweenTimeString = smallestBreak + " minutes";
        } else {
            betweenTimeString = SchedulerTools.getTimeString(smallestBreak);
        }
        return betweenTimeString;
    }

    public DetailedScheduleGrid getScheduleGrid() { return grid; }

    public Schedule getSchedule() { return schedule; }

    public void printSchedule() {
        SchedulePrinter printer = new SchedulePrinter(session, schedule);
        printer.setOriginalGrid(grid);
        UITools.printSchedule(this, printer);
    }

    private static class LegendIcon implements Icon {
        private Paint paint = null;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (paint == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(paint);
            int fourthx = getIconWidth()*3/16;
            int fourthy = getIconHeight()*3/16;
            int ox = x+fourthx;
            int oy = y+fourthy;
            int ow = getIconWidth()-fourthx*2;
            int oh = getIconHeight()-fourthy*2;
            g2.fillOval(ox, oy, ow, oh);
            g2.setColor(Color.BLACK);
            Object oldaa = g2.getRenderingHint(KEY_ANTIALIASING);
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.drawOval(ox, oy, ow, oh);
            g2.setRenderingHint(KEY_ANTIALIASING, oldaa);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }

        public void setPaint(Paint paint) {
            this.paint = paint;
        }
    }
}
