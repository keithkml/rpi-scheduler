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
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.StringFormatType;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public class DetailedSchedulePainter extends TimeGridSchedulePainter {
    {
        setDefaultPaint(null);
    }

    public DetailedSchedulePainter(SchedulingSession session, TimeGrid grid) {
        super(session, grid);
    }

    public Paint getPaint(UniqueSection section) {
        Paint paint = super.getPaint(section);
        if (paint == null) {
            paint = getNewPaint();
            addPaint(section, paint);
        }
        return paint;
    }

    private Paint getNewPaint() {
        Map<UniqueSection,Paint> paints = getPaints();
        Color paint = null;
        for (Color color : DetailedScheduleGrid.COLORS_INITIAL) {
            if (!paints.containsValue(color)) {
                paint = color;
                break;
            }
        }
        if (paint == null) paint = DetailedScheduleGrid.getRandomLightColor();
        return paint;
    }

    protected void paintSectionFG(Graphics2D g, VisualSection section,
            DailyTimePeriod period) {
        SectionDescriptor sectionDesc = section.getSectionDesc();

        Rectangle2D box = getBox(period);
        int x = (int) box.getX();
        int y = (int) box.getY();
        int bw = ((int) (box.getX() + box.getWidth())) - x - 1;
        int bh = ((int) (box.getY() + box.getHeight())) - y - 1;

        CourseDescriptor cd = CourseDescriptor.getInstanceForSection(sectionDesc);
        SchedulerUIPlugin plugin = getSession().getUIPlugin();
        String name = plugin.getCourseString(cd, StringFormatType.COURSE_VERY_SHORT);

        Font font = UITools.getLabelFont();
        int descent = 0;
        int width = 0;
        for (int size = 12; size >= 1; size--) {
            font = font.deriveFont(Font.PLAIN, size);
            FontMetrics fm = g.getFontMetrics(font);
            descent = fm.getMaxDescent();
            width = fm.stringWidth(name);
            if (fm.getMaxAscent() + descent <= bh && width <= bw - 2) {
                break;
            }
        }

        g.setFont(font);
        g.drawString(name, x + 1 + ((float) bw / 2) - ((float) width / 2),
                y + ((float) bh / 2) + (font.getSize2D() / 2));
    }

}
