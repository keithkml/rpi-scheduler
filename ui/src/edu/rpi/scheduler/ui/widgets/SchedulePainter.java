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

import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.DefensiveTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public abstract class SchedulePainter {
    private SchedulingSession session;
    private List<VisualUnique> sections = Collections.emptyList();
    private Random random;
    private Schedule schedule = null;
    private Map<PeriodHolder, Rectangle2D> boxMap
            = new HashMap<PeriodHolder, Rectangle2D>(20);;

    protected SchedulePainter(SchedulingSession session) {
        this.session = session;
    }

    public SchedulingSession getSession() {
        return session;
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
    }

    public synchronized void setSchedule(Schedule schedule) {
        if (schedule != null) {
            Collection<UniqueSection> uniques = schedule.getSections();
            setSections(uniques);
        } else {
            sections = Collections.emptyList();
        }
    }

    public synchronized void setSections(Collection<UniqueSection> uniques) {
        DefensiveTools.checkNull(uniques, "uniques");

        List<VisualUnique> visUniques
                = new ArrayList<VisualUnique>(uniques.size());

        for (UniqueSection uniqueSection : uniques) {
            Collection<SectionDescriptor> sections
                    = uniqueSection.getSectionDescriptors();
            List<VisualSection> array
                    = new ArrayList<VisualSection>(sections.size());
            for (SectionDescriptor section : sections) {
                array.add(new VisualSection(session, section));
            }

            visUniques.add(new VisualUnique(uniqueSection, array, getPaint(uniqueSection)));
        }

        updatePaints();

        this.schedule = null;
        sections = visUniques;
    }

    protected void updatePaints() {
        for (VisualUnique vu : this.sections) {
            vu.setPaint(getPaint(vu.getUniqueSection()));
        }
    }

    public Map<PeriodHolder, Rectangle2D> getBoxMap() {
        return new HashMap<PeriodHolder, Rectangle2D>(boxMap);
    }

    public synchronized final Schedule getSchedule() { return schedule; }

    protected void paintSectionBG(Graphics2D g, VisualUnique unique) {
        VisualSection section = unique.getCurrentSection();
        Paint paint = unique.getPaint();

        for (int day = 0; day < 7; day++) {
            List<DailyTimePeriod> periods = section.getPeriods(day);

            for (DailyTimePeriod period : periods) {
                Rectangle2D box = getBox(period);
                boxMap.put(new PeriodHolder(unique.getUniqueSection(), period), box);

                int x = (int) box.getX();
                int y = (int) box.getY();
                int bw = ((int) (box.getX() + box.getWidth())) - x;
                int bh = ((int) (box.getY() + box.getHeight())) - y - 1;

                g.setPaint(paint);
                g.fillRect(x, y, bw, bh);

                g.setColor(Color.BLACK);
                g.drawRect(x, y, bw, bh);
                if (unique.isSelected()) g.drawRect(x+1, y+1, bw-2, bh-2);

                paintSectionFG(g, section, period);
            }
        }
    }

    protected void paintSectionFG(Graphics2D g, VisualSection section,
            DailyTimePeriod period) {
        // does nothing
    }

    protected synchronized Paint getPaint(UniqueSection section) {
        if (random == null) random = new Random();
        return new Color(random.nextInt(256), random.nextInt(256),
                random.nextInt(256));
    }

    /**
     * @param period a period which is on for one and only one day
     */
    protected abstract Rectangle2D getBox(DailyTimePeriod period);

    public void paint(Graphics2D g) {
        boxMap.clear();
        List<VisualUnique> sections = this.sections;
        for (VisualUnique visualUnique : sections) {
            paintSectionBG(g, visualUnique);
        }
    }

    public List<VisualUnique> getSections() { return sections; }

    public static class PeriodHolder {
        private final UniqueSection section;
        private final DailyTimePeriod period;

        private PeriodHolder(UniqueSection section, DailyTimePeriod period) {
            this.section = section;
            this.period = period;
        }

        public UniqueSection getSection() { return section; }

        public DailyTimePeriod getPeriod() { return period; }

        public int hashCode() {
            return getSection().hashCode() ^ getPeriod().hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PeriodHolder)) return false;
            PeriodHolder ph = (PeriodHolder) obj;
            return (ph.getSection().equals(getSection())
                    && ph.getPeriod().equals(getPeriod()));
        }
    }
}
