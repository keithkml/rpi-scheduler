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

import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.TimeGridType;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

public class MiniSchedulePanel extends JPanel {
    private SchedulePainter painter;
    private SchedulePaintProvider paintProvider;

    private SchedulingSession session;
    private DailyTimePeriod displayedRange;

    private static final Color COLOR_GRID = new Color(220, 220, 220);

    {
        setOpaque(false);
        setDoubleBuffered(true);
    }

    public MiniSchedulePanel(SchedulePaintProvider paintProvider) {
        this(paintProvider, null);
    }


    public MiniSchedulePanel(SchedulePaintProvider paintProvider,
            SchedulingSession session) {
        this.paintProvider = paintProvider;
        if (session != null) setSession(session);
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
        painter = new MiniSchedulePainter(session);
        SchedulerUIPlugin plugin = session.getUIPlugin();
        //TOSMALL: make displayed range change with selected schedule
        displayedRange = plugin.getPreferredTimeGridRange(
                TimeGridType.SCHEDULE_LIST);
    }

    public void setSchedule(Schedule schedule) {
        painter.setSchedule(schedule);
    }

    public void setSections(Collection<UniqueSection> sections) {
        painter.setSections(sections);
    }

    public SchedulePainter getPainter() { return painter; }

    private float getXFromDayNumber(int day) {
        if (!displayedRange.isOnDay(day)) {
            throw new IllegalArgumentException("this grid does not have day "
                    + day + " enabled");
        }
        int col = DailyTimePeriod.getColumnFromDayNumber(displayedRange, day);
        Insets insets = getInsets();
        int left = insets.left;
        int width = getWidth() - left - insets.right;
        return left + (col * width / displayedRange.getDayCount());
    }

    private float getYFromTime(Time time) {
        int mins = time.getMinutesFromMidnight();
        int rangeStart = displayedRange.getStart().getMinutesFromMidnight();
        int rangeLength = displayedRange.getPeriod().getElapsedMinutes();
        Insets insets = getInsets();
        int top = insets.top;
        int htrange = getHeight() - top - insets.bottom;
        return top + (htrange  * (mins - rangeStart) / rangeLength);
    }

    protected void paintComponent(Graphics og) {
        super.paintComponent(og);

        Graphics2D g = (Graphics2D) og;

//
//        g.setColor(COLOR_GRID);
//        for (int day = 0; day < 7; day++) {
//            Rectangle2D.Float rect = painter.getTimeRect(day, 0);
//            g.drawLine((int) rect.x, 0, (int) rect.x, getHeight());
//        }
//        for (int block = 0; block < 32; block++) {
//            Rectangle2D.Float rect = painter.getTimeRect(0, block);
//            g.drawLine(0, (int) rect.y, getWidth(), (int) rect.y);
//        }

        painter.paint(g);
    }

    private class MiniSchedulePainter extends SchedulePainter {
        public MiniSchedulePainter(SchedulingSession session) {
            super(session);
        }

        protected Paint getPaint(UniqueSection course) {
            return paintProvider.getPaint(course);
        }

        protected Rectangle2D getBox(DailyTimePeriod period) {
            int day = period.getDay();
            float x = getXFromDayNumber(day);
            float w = getXFromDayNumber(day+1)-x;

            float y = getYFromTime(period.getStart());
            float h = getYFromTime(period.getEnd())-y;

            return new Rectangle2D.Float(x, y, w, h);
        }
    }
}
