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
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.TimeGridType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Set;

public class SingleCourseGrid extends ScheduleGrid {
    private CourseDescriptor course = null;
    private SectionDescriptor section = null;
    private Paint currentPaint = Color.RED;
    private Paint otherPaint = new Color(240, 240, 240);

    public Paint getOtherPaint() { return otherPaint; }

    public void setOtherPaint(Paint otherPaint) { this.otherPaint = otherPaint; }

    public Paint getCurrentPaint() { return currentPaint; }

    public void setCurrentPaint(Paint currentPaint) { this.currentPaint = currentPaint; }

    public CourseDescriptor getCourse() { return course; }

    public void setCourse(CourseDescriptor course) {
        this.course = course;
        setSection(null);
        repaint();
    }

    public void setSection(SectionDescriptor section) {
        this.section = section;
        Set<UniqueSection> sections;
        if (section == null) {
            sections = Collections.emptySet();
        } else {
            sections = Collections.singleton(UniqueSection.getInstance(section));
        }
        setSections(sections);
        repaint();
    }

    public synchronized void paintBetweenGrid(Graphics2D g) {
        SchedulingSession session = getSession();
        if (session == null || course == null) return;

        Paint inactivePaint = otherPaint;
        g.setPaint(inactivePaint);
        for (Section section : course.getActualCourse().getSections()) {
            fillSection(g, section);
        }
    }

    private void fillSection(Graphics2D g, Section section) {
        Color fill = g.getColor();

        for (ClassPeriod period : section.getPeriods()) {
            DailyTimePeriod timePeriod = period.getTimePeriod();
            Time start = timePeriod.getStart();

            Time end = timePeriod.getEnd();

            int day = 0;
            for (boolean on : timePeriod.getDays()) {
                if (on) {
                    Rectangle2D.Float rect = getTimeRect(day, start, end);

                    g.setColor(fill);
                    g.fill(rect);
                }
                day++;
            }
        }
        g.setColor(fill);
    }

    protected TimeGridType getTimeGridType() {
        return TimeGridType.COURSE_PREVIEW;
    }
}
