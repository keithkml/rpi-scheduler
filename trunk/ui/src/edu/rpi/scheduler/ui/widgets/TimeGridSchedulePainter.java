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

import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.DefensiveTools;

import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TimeGridSchedulePainter extends SchedulePainter {
    private final TimeGrid grid;
    private Paint defaultPaint = null;
    private Map<UniqueSection,Paint> paints;

    public TimeGridSchedulePainter(SchedulingSession session, TimeGrid grid) {
        super(session);

        DefensiveTools.checkNull(grid, "grid");

        this.grid = grid;

        setPaints(Collections.<UniqueSection,Paint>emptyMap());
    }

    public void addPaint(UniqueSection section, Paint paint) {
        paints.put(section, paint);
    }

    public Map<UniqueSection,Paint> getPaints() {
        return Collections.unmodifiableMap(new HashMap<UniqueSection, Paint>(
                paints));
    }

    public void setPaints(Map<UniqueSection,Paint> paints) {
        this.paints = new HashMap<UniqueSection, Paint>(paints);
        updatePaints();
        grid.repaint();
    }

    public Paint getDefaultPaint() { return defaultPaint; }

    public void setDefaultPaint(Paint defaultPaint) {
        this.defaultPaint = defaultPaint;
        updatePaints();
        grid.repaint();
    }

    public TimeGrid getTimeGrid() { return grid; }

    protected Rectangle2D.Float getTimeRect(int day, int block) {
        return grid.getTimeRect(day, block);
    }

    public Paint getPaint(UniqueSection section) {
        Paint paint = paints.get(section);
        if (paint == null) return defaultPaint;
        else return paint;
    }

    protected Rectangle2D getBox(DailyTimePeriod period) {
        return grid.getTimeRect(period.getDay(),
                period.getStart(), period.getEnd());
    }
}
