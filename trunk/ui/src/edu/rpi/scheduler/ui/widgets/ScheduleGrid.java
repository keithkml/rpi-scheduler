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

import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.DefensiveTools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.Collection;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

public abstract class ScheduleGrid extends TimeGrid {
    public static final String PROP_SELECTED_SCHEDULE = "selectedSchedule";

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private TimeGridSchedulePainter schedPainter;
    private Schedule schedule = null;

    protected ScheduleGrid() {
        this(null, null);
    }

    protected ScheduleGrid(SchedulingSession session) {
        this(session, null);
    }

    protected ScheduleGrid(SchedulingSession session,
            TimeGridSchedulePainter schedPainter) {
        super(session);
        if (schedPainter == null) {
            this.schedPainter = new TimeGridSchedulePainter(session, this);
        } else {
            this.schedPainter = schedPainter;
        }
    }

    protected synchronized void setSections(Collection<UniqueSection> sections) {
        DefensiveTools.checkNull(sections, "sections");

        this.schedule = null;
        schedPainter.setSession(getSession());
        schedPainter.setSections(sections);

        repaint();
    }

    protected synchronized void setSchedule(Schedule schedule) {
        if (this.schedule == schedule) return;
        
        this.schedule = schedule;
        schedPainter.setSession(getSession());
        schedPainter.setSchedule(schedule);

        repaint();
    }

    public void setSelectedSection(UniqueSection section) {
        boolean repaint = false;
        for (VisualUnique vu : schedPainter.getSections()) {
            boolean oldSelected = vu.isSelected();
            boolean newSelected = vu.getUniqueSection().equals(section);
            if (oldSelected != newSelected) {
                repaint = true;
                vu.setSelected(newSelected);
            }
        }
        if (repaint) repaint();

        pcs.firePropertyChange(PROP_SELECTED_SCHEDULE, null, section);
    }

    public void addSelectedSectionListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(PROP_SELECTED_SCHEDULE, l);
    }

    public void removeSelectedSectionListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(PROP_SELECTED_SCHEDULE, l);
    }

    /**
     * May return null if no schedule is set, just sections
     */
    public Schedule getSchedule() { return schedule; }

    public final TimeGridSchedulePainter getSchedulePainter() {
        return schedPainter;
    }

    public Paint getPaint(UniqueSection section) {
        return schedPainter.getPaint(section);
    }

    protected final void setSchedulePainter(TimeGridSchedulePainter schedPainter) {
        this.schedPainter = schedPainter;
    }

    public synchronized void paintComponent(Graphics og) {
        super.paintComponent(og);

        Graphics2D g = (Graphics2D) og;

        updateGridDimensions();

        paintGridBG(g);
        paintBetweenGrid(g);
        paintGridFG(g);

        // draw boxes
        schedPainter.paint(g);
    }

    protected synchronized void updateGridDimensions() {
        int gridWidth = getWidth();
        int gridHeight = getHeight();

        initGrid(gridWidth, gridHeight);
    }

    protected void paintBetweenGrid(Graphics2D g) {
        // does nothing
    }
}
