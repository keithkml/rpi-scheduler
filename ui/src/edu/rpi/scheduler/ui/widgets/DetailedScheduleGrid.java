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

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.Duration;
import edu.rpi.scheduler.schedb.WeekMask;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.spec.DayMask;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.TimeRepresentation;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.TimeGridType;
import edu.rpi.scheduler.ui.UITools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;


public class DetailedScheduleGrid extends ScheduleGrid {
    private static final Color COLOR_BLOCKEDTIME = new Color(230, 230, 230);
    private static final Font FONT_GRID = new Font("dialog", Font.PLAIN, 10);
    static final Color[] COLORS_INITIAL = new Color[] {
        new Color(226, 174, 236), new Color(249, 217, 184),
        new Color(249, 173, 173), new Color(167, 241, 135),
        new Color(237, 243, 255), new Color(162, 202, 243),
        new Color(255, 247, 153),
    };
    private static final Random random = new Random();

    private final NumberFormat format = NumberFormat.getNumberInstance();
    {
        format.setMinimumIntegerDigits(2);

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                SchedulePainter.PeriodHolder inside = getPeriodAtPoint(point);
                if (inside == null) {
                    setCursor(null);
                    setToolTipText(null);
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    UniqueSection us = inside.getSection();
                    Course course = us.getCourseDescriptor().getActualCourse();
                    List<SectionDescriptor> sorted
                            = new ArrayList<SectionDescriptor>(
                                    us.getSectionDescriptors());
                    Collections.sort(sorted);
                    DailyTimePeriod period = inside.getPeriod();
                    setToolTipText("<HTML>" + course.getName() + "<BR>"
                            + "Section"
                            + (sorted.size() == 1 ? "" : "s") + " "
                            + UITools.getSectionNumberList(sorted) + "<BR>"
                            + period.getStart() + " to " + period.getEnd());
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                SchedulePainter.PeriodHolder period = getPeriodAtPoint(e.getPoint());
                if (period != null) {
                    setSelectedSection(period.getSection());
                } else {
                    setSelectedSection(null);
                }
            }
        });
    }

    static Color getRandomLightColor() {
        return new Color(128+random.nextInt(128),
                128+random.nextInt(128),
                128+random.nextInt(128));
    }

    private SchedulePainter.PeriodHolder getPeriodAtPoint(Point point) {
        Map<SchedulePainter.PeriodHolder, Rectangle2D> boxMap
                = getSchedulePainter().getBoxMap();
        int x = point.x;
        int y = point.y;
        SchedulePainter.PeriodHolder inside = null;
        for (Map.Entry<SchedulePainter.PeriodHolder, Rectangle2D> entry
                : boxMap.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                inside = entry.getKey();
            }
        }
        return inside;
    }

    private WeekMask<?> blockedTimeMask;
    private List<CourseDescriptor> courses = new ArrayList<CourseDescriptor>();
    private Map<CourseDescriptor,VisualUnique> courseToSection
            = new HashMap<CourseDescriptor, VisualUnique>();

    private long seed = 0;

    public DetailedScheduleGrid() {
        this(null);
    }

    public DetailedScheduleGrid(SchedulingSession session) {
        super(session);
        setSchedulePainter(new DetailedSchedulePainter(session, this));
    }

    protected TimeGridType getTimeGridType() {
        return TimeGridType.SCHEDULE;
    }

    protected void initSession() {
        SchedulingSession session = getSession();
        DataContext dc = session.getDataContext();
        SchedulerDataPlugin plugin = dc.getSchedulerPlugin();
        SchedulerEngine engine = session.getEngine();
        WeekMask<?> mask = null;
        if (engine != null) mask = engine.getBlockedTime();
        if (mask == null) mask = plugin.getTimeRepresentation().newWeekMask();
        blockedTimeMask = mask;
    }

    public synchronized void setSchedule(Schedule schedule) {
        super.setSchedule(schedule);
        if (schedule != null) {
            random.setSeed(seed);

            Map<CourseDescriptor,VisualUnique> coursetosect;
            coursetosect = this.courseToSection;
            coursetosect.clear();

            List<VisualUnique> uniques = getSchedulePainter().getSections();
            for (VisualUnique section : uniques) {
                coursetosect.put(section.getCourseDesc(), section);
            }
        }
        repaint();
    }

    public synchronized void updateScheduleList() {
        seed = System.currentTimeMillis();

        SchedulerEngine scheduler = getScheduler();
        List<Schedule> schedules = scheduler.getGeneratedSchedules();

        updateCourses();

        LinkedList<Color> colors = new LinkedList<Color>(
                Arrays.asList(COLORS_INITIAL));
        Collections.shuffle(colors, random);

        if (schedules.size() > 0) {
            Collection<CourseDescriptor> courses = scheduler.getSelectedCourseDescriptors();
            Map<UniqueSection,Paint> paints;
            paints = new HashMap<UniqueSection, Paint>(courses.size());
            for (CourseDescriptor course : courses) {
                Color color;
                if (colors.size() == 0) {
                    //TOMAYBE: colors should never be too close to other colors
                    color = getRandomLightColor();
                } else {
                    color = colors.removeFirst();
                }
                Paint paint;
                if (scheduler.isExtra(course)) paint = new PolkaPaint(color);
                else paint = color;

                for (Schedule schedule : schedules) {
                    for (UniqueSection section : schedule.getSections()) {
                        if (section.getCourseDescriptor().equals(course)) {
                            paints.put(section, paint);
                        }
                    }
                }
            }

            getSchedulePainter().setPaints(paints);
        }

        setSchedule(null);
    }

    private synchronized void updateCourses() {
        SchedulerEngine scheduler = getScheduler();
        List<CourseDescriptor> req = scheduler.getRequiredCourses();
        List<CourseDescriptor> opt = scheduler.getExtraCourses();
        List<CourseDescriptor> courses;
        courses = new ArrayList<CourseDescriptor>(req.size() + opt.size());
        Collections.sort(req);
        Collections.sort(opt);
        courses.addAll(req);
        courses.addAll(opt);
        this.courses = courses;
    }

    /**
     * Sets the time mask the user blocked, as a nice touch in displaying
     * schedules.
     * @param blockedTime the time mask blocked by the user
     */
    public synchronized void setBlockedTime(WeekMask<?> blockedTime) {
        assert blockedTime != null;

        this.blockedTimeMask = blockedTime;

        repaint();
    }

    protected synchronized void updateGridDimensions() {
        int gridWidth = getWidth();
        int gridHeight = getHeight() - 35;

        initGrid(gridWidth, gridHeight);
    }

    public synchronized void paintComponent(Graphics og) {
        Graphics2D g = (Graphics2D) og;

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(FONT_GRID);

        super.paintComponent(g);

        drawColumnFooters(g);
    }

    private synchronized void drawColumnFooters(Graphics2D g) {
        List<VisualUnique> sections = getSchedulePainter().getSections();
        int gridHeight = getGridHeight();

        // print day-specific information
        g.setColor(Color.BLACK);
        g.setFont(FONT_GRID);

        int[] classes = new int[7];
        int[] dayTotal = new int[7];
        for (VisualUnique sec : sections) {
            VisualSection section = sec.getCurrentSection();

            for (int day = 0; day < 7; day++) {
                List<DailyTimePeriod> periods = section.getPeriods(day);
                classes[day] += periods.size();
                for (DailyTimePeriod period : periods) {
                    dayTotal[day] += period.getDuration().getTotalMinutes();
                }
            }
        }

        FontMetrics metrics = g.getFontMetrics();
        for (int i = 0; i < 7; i++) {
            if (classes[i] == 0) continue;

            int col = getColumnFromDayNumber(i);
            float x = getXFromColumn(col);
            float width = getXFromColumn(col + 1) - x;

            String classesText = String.valueOf(classes[i]) + " class"
                    + (classes[i] == 1 ? "" : "es");
            String total = new Duration(dayTotal[i]) + " total";

            int classesWidth = metrics.stringWidth(classesText)/2;
            g.drawString(classesText, x + width/2 - classesWidth,
                    gridHeight + 10);

            int totalWidth = metrics.stringWidth(total)/2;
            g.drawString(total, x + width/2 - totalWidth, gridHeight + 22);
        }
    }

    protected synchronized void paintBetweenGrid(Graphics2D g) {
        // draw blocked sections
        WeekMask<?> blockedTimeMask = this.blockedTimeMask;
        g.setColor(COLOR_BLOCKEDTIME);

        if (blockedTimeMask == null) return;

        DayMask firstDay = blockedTimeMask.getDayMask(0);
        int max = firstDay.getMaxBlockNum();
        for (int day = 0; day < 7; day++) {
            DayMask mask = blockedTimeMask.getDayMask(day);
            if (mask.isEmpty()) continue;

            for (int block = getFirstHourBlock();
                 block <= max && block <= getLastHourBlock(); block++) {
                if (mask.isOn(block)) g.fill(getTimeRect(day, block));
            }
        }
    }


    protected static class PolkaPaint extends TexturePaint {
        private static final int DOT_SIZE = 4;
        private static final int TEXTURE_SIZE = DOT_SIZE*2;
        private static final Rectangle RECT = new Rectangle(TEXTURE_SIZE, TEXTURE_SIZE);

        private final Color color;

        public PolkaPaint(Color color) {
            super(genSlashyImg(color), RECT);

            this.color = color;
        }

        public Color getColor() { return color; }

        private static BufferedImage genSlashyImg(Color color) {
            BufferedImage img = new BufferedImage(
                    TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_3BYTE_BGR);


            Graphics2D g = img.createGraphics();
            g.setBackground(color);
            g.clearRect(0, 0, img.getWidth(), img.getHeight());

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.drawLine(0, 0, img.getWidth(), img.getHeight());

            return img;
        }

        private static BufferedImage genStippledImg(Color color) {
            BufferedImage img = new BufferedImage(
                    2, 2, BufferedImage.TYPE_3BYTE_BGR);

            img.setRGB(0, 0, 0xffffffff);
            img.setRGB(1, 1, 0xffffffff);
            int crgb = color.getRGB();
            img.setRGB(0, 1, crgb);
            img.setRGB(1, 0, crgb);

            return img;
        }

        private static BufferedImage genPolkaImg(Color color) {
            BufferedImage img = new BufferedImage(
                    TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g = img.createGraphics();

            g.setBackground(color);
            g.clearRect(0, 0, img.getWidth(), img.getHeight());

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.fillOval(0, 0, DOT_SIZE, DOT_SIZE);
            g.fillOval(DOT_SIZE, DOT_SIZE, DOT_SIZE, DOT_SIZE);
            return img;
        }
    }
}
