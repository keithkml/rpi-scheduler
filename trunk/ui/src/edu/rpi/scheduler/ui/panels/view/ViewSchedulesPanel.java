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

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.Duration;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.WeekMask;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.TimeRepresentation;
import edu.rpi.scheduler.ui.RankingMethod;
import edu.rpi.scheduler.ui.ScheduleInfo;
import edu.rpi.scheduler.ui.SchedulePresenter;
import edu.rpi.scheduler.ui.panels.DefaultSubpanel;
import edu.rpi.scheduler.ui.widgets.MiniSchedulePanel;
import edu.rpi.scheduler.ui.widgets.ScheduleGrid;
import edu.rpi.scheduler.ui.widgets.SchedulePaintProvider;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

public class ViewSchedulesPanel
        extends DefaultSubpanel implements SchedulePresenter {
    //TOMAYBE: only compute one schedule in preEnter; compute the rest live
    //TOMAYBE: show info for multiple sections with same time less redundantly
    private JPanel mainPanel;
    private ScheduleViewer scheduleViewer;
    private JLabel infoText;
    private JList scheduleList;

    private List<Schedule> schedules;
    private ScheduleListModel schedListModel = new ScheduleListModel();

    private boolean inited = false;

    private NumberFormat numSchedulesFormat = NumberFormat.getIntegerInstance();
    private String schedulesFoundText;
    private JComboBox rankByBox;
    private JLabel rankByLabel;

    public String getNextButtonText() {
        return "Generate schedules";
    }

    public String getPrevButtonText() {
        return "View schedules";
    }

    public void handleEnteringError(Throwable t) {
        if (t instanceof OutOfMemoryError || t instanceof StackOverflowError) {
            String opt = getEngine().getExtraCourses().size() > 1
                    ? " or selecting fewer \"extra\" courses" : "";
            JOptionPane.showMessageDialog(getSchedulerPanel(),
                    "There are too many possible schedules to fit into your "
                    + "computer's memory.\n\nTry narrowing down your schedules"
                    + opt + ".",
                    "Out of Memory", JOptionPane.ERROR_MESSAGE);
        } else {
            super.handleEnteringError(t);
        }
    }

    public String getPanelTitle() {
        if (schedulesFoundText == null) return "Your possible schedules";
        else return schedulesFoundText;
    }

    public boolean preEnter() {
        SchedulerEngine scheduler = getEngine();

        scheduler.generateSchedules();

        if (scheduler.getGeneratedSchedules().size() == 0) {
            showNoSchedulesMessage();
            return false;
        } else {
            return true;
        }
    }

    private void showNoSchedulesMessage() {
        SchedulerEngine scheduler = getEngine();
        WeekMask<?> blocked = scheduler.getBlockedTime();
        boolean blockedTime = !blocked.isEmpty();
        boolean blockedSections = !scheduler.getBlockedSections().isEmpty();
        int reqcourses = scheduler.getRequiredCourses().size();
        int optcourses = scheduler.getExtraCourses().size();
        String reqstr = (optcourses > 0 ? "non-extra " : "");

        String reason;
        if (reqcourses > 8) {
            SchedulerDataPlugin plugin = getSession().getDataPlugin();
            int blocks = blocked.getTimeBlockSum();
            TimeRepresentation timeRep = plugin.getTimeRepresentation();
            Duration duration = timeRep.getDuration(blocks);
            int hours = duration.getHours();
            String timeStr;
            if (hours > 2) {
                timeStr = " and " + hours + " hours of blocked class\ntime";
            } else {
                timeStr = "";
            }

            reason = "Hey, oddly enough, with " + reqcourses + " " + reqstr +
                    "courses\nchosen" + timeStr + ", there aren't any " +
                    "possible schedules,";

        } else if (blockedSections && blockedTime) {
            reason = "Sorry, but no schedules are possible with the\n"
                    + "times and sections you selected. Try blocking out\n"
                    + "fewer " + reqstr + "sections or a smaller range of time.";

        } else if (blockedSections) {
            reason = "Sorry, but no schedules are possible given the\n"
                    + "courses and sections you specified. Try blocking\n"
                    + "fewer sections "
                    + (optcourses > 0 ? "in required classes " : "")
                    + "and try again.";

        } else if (blockedTime) {
            reason = "Sorry, but no schedules are possible given the\n"
                    + reqstr + "courses you selected and the time constraints\n"
                    + "you made. Try reducing the amount of blocked out time.";

        } else {
            reason = "Sorry, but the combination of " + reqstr
                    + "courses you chose\n"
                    + "can not be taken together, as no schedules are\n"
                    + "possible without two overlapping.";
        }

        JOptionPane.showMessageDialog(getSchedulerPanel(), reason,
                "No possible schedules", JOptionPane.WARNING_MESSAGE);
    }

    public void entering() {
        SchedulerEngine engine = getEngine();

        scheduleViewer.getScheduleGrid().setBlockedTime(engine.getBlockedTime());
        schedules = engine.getGeneratedSchedules();
        scheduleViewer.getScheduleGrid().updateScheduleList();
        schedListModel.updateSchedules();

        int numSchedules = schedules.size();
        boolean moreThanOneSchedule = numSchedules > 1;
        rankByLabel.setEnabled(moreThanOneSchedule);
        rankByBox.setEnabled(moreThanOneSchedule);
        String num = numSchedulesFormat.format(numSchedules);

        String s = (numSchedules == 1 ? "" : "s");
        schedulesFoundText = num + " schedule" + s + " found";
        infoText.setText(schedulesFoundText);
        if (scheduleList.getSelectedIndex() ==
                -1 && schedListModel.getSize() > 0) {
            scheduleList.setSelectedIndex(0);
        }

        rankByBox.setSelectedItem(engine.getSortMethod());
    }

    public boolean canProgress() { return true; }

    public String getInfoBarText() {
        int numSchedules = schedules.size();
        if (numSchedules > 30) {
            return "The Scheduler generated a lot of schedules for you. You "
                    + "may want to narrow down this list by blocking out times "
                    + "and sections on the previous page.";
        } else {
            return null;
        }
    }

    public synchronized void init() {
        if (inited) return;
        inited = true;

        setLayout(new BorderLayout());
        add(mainPanel);

        scheduleViewer.init();
        scheduleViewer.setSession(getSession());

        numSchedulesFormat.setGroupingUsed(true);


        scheduleList.setModel(schedListModel);
        scheduleList.setVisibleRowCount(Integer.MAX_VALUE/2);
        scheduleList.setCellRenderer(new ListCellRenderer() {
            private MiniSchedulePanel panel
                    = new MiniSchedulePanel(new PaintProvider(),
                            getSession());

            private final LineBorder BORDER_WHITE
                    = new LineBorder(Color.WHITE, 10);
            private final LineBorder BORDER_BLACK
                    = new LineBorder(Color.BLACK, 1);
            private final CompoundBorder BORDER_COMP
                    = new CompoundBorder(BORDER_WHITE, BORDER_BLACK);

            private final Dimension dimension = new Dimension(100, 80);

            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                panel.setPreferredSize(dimension);
                panel.setBackground(scheduleList.getSelectionBackground());
                panel.setOpaque(isSelected);
                panel.setSchedule((Schedule) value);
                panel.setBorder(BORDER_COMP);

                return panel;
            }
        });
        ListSelectionModel selModel = scheduleList.getSelectionModel();
        selModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Schedule sched = (Schedule) scheduleList.getSelectedValue();
                scheduleViewer.setSchedule(sched);
            }
        });

        rankByBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value != null && !(value instanceof RankingMethod)) {
                    return super.getListCellRendererComponent(list, value,
                            index, isSelected, cellHasFocus);
                }

                RankingMethod rm = (RankingMethod) value;
                String string;
                if (rm == null) string = "None";
                else string = rm.getName();
                return super.getListCellRendererComponent(list, string, index,
                        isSelected, cellHasFocus);
            }
        });
        for (RankingMethod method : getSession().getUIPlugin().getRankingMethods()) {
            rankByBox.addItem(method);
        }
        rankByBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item instanceof RankingMethod) {
                        Object selected = scheduleList.getSelectedValue();
                        getEngine().sortBy(((RankingMethod) item).getComparator());
                        if (selected != null) {
                            scheduleList.setSelectedValue(selected, true);
                        }
                    }
                }
            }
        });

        scheduleViewer.setSelectedSection(null);
    }

    public ScheduleInfo getCurrentSchedule() {
        return new ScheduleInfo(getSession(), scheduleViewer.getSchedule());
    }

    public ScheduleGrid getScheduleGrid() {
        return scheduleViewer.getScheduleGrid();
    }

    private class ScheduleListModel extends AbstractListModel {
        private List schedcopy;

        {
            clear();
        }

        private void clear() {
            schedcopy = Collections.EMPTY_LIST;
        }

        public int getSize() {
            return schedules.size();
        }

        public Object getElementAt(int index) {
            return schedules.get(index);
        }

        public void updateSchedules() {
            int size = schedcopy.size();
            clear();
            if (size > 0) fireIntervalRemoved(this, 0, size-1);

            List<Schedule> schedules = ViewSchedulesPanel.this.schedules;

            if (schedules != null) {
                schedcopy = schedules;
                fireIntervalAdded(this, 0, schedules.size()-1);
            }
        }
    }

    private class PaintProvider implements SchedulePaintProvider {
        public Paint getPaint(UniqueSection section) {
            return scheduleViewer.getScheduleGrid().getPaint(section);
        }
    }
}
