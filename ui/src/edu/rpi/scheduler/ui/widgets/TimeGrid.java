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
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.TimeRepresentation;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.TimeGridType;
import edu.rpi.scheduler.ui.UITools;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import static edu.rpi.scheduler.schedb.spec.TimeRepresentation.Bias.LATER;
import static edu.rpi.scheduler.schedb.spec.TimeRepresentation.Bias.EARLIER;
import static edu.rpi.scheduler.schedb.Time.AM;
import static edu.rpi.scheduler.schedb.Time.PM;

/**
 * Provides an interface for displaying and manipulating a time grid.
 */
public abstract class TimeGrid extends JPanel {
    private static final Stroke PLAIN_STROKE = new BasicStroke(1);
    private static final Stroke DASH_STROKE = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0,
            new float[] { 3, 3 }, 0);

    private static final String[] DAY_NAMES_SHORT = {
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };
    private static final String[] DAY_NAMES = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"
    };

    private static final DailyTimePeriod DEFAULT_GRID_RANGE
            = new DailyTimePeriod(new boolean[] {
        true, true, true, true, true, true, false
    }, new Time(6, 00, Time.AM), new Time(11, 00, Time.PM));

    private static final int offx = 20;
    private static final int offy = 20;
    private static final int margin = 2;
    private static final int subx = offx+margin;
    private static final int suby = offy+margin;

    private int width = -1;
    private int height = -1;
    private float dayWidth = -1;
    private float blockHeight = -1;

    private SchedulingSession session = null;
    private DailyTimePeriod displayedRange = DEFAULT_GRID_RANGE;
    private int lastHourBlock;
    private int firstHourBlock;

    {
        setOpaque(false);
    }


    protected TimeGrid() {
        this(null);
    }

    protected TimeGrid(SchedulingSession session) {
        if (session != null) setSession(session);
    }

    /**
     * Returns the width that the time grid is taking up on the screen.
     * @return the width on the screen of the time grid
     */
    private int getGridWidth() { return width; }

    /**
     * Returns the height of the time grid on the screen.
     * @return the height of the time grid on the screen
     */
    protected final int getGridHeight() { return height; }

    protected final float getXFromColumn(int col) {
        return offx + (dayWidth * col);
    }

    protected final float getXFromDayNumber(int day) {
        if (!displayedRange.isOnDay(day)) {
            throw new IllegalArgumentException("this grid does not have day "
                    + day + " enabled");
        }
        return getXFromColumn(getColumnFromDayNumber(day));
    }

    public DailyTimePeriod getDisplayedRange() { return displayedRange; }

    protected int getColumnFromDayNumber(int day) {
        return DailyTimePeriod.getColumnFromDayNumber(displayedRange, day);
    }

    protected int getYFromTime(Time time) {
        int mins = time.getMinutesFromMidnight();
        int rangeStart = displayedRange.getStart().getMinutesFromMidnight();
        int rangeLength = displayedRange.getPeriod().getElapsedMinutes();
        return offy + ((getGridHeight() - offy)  * (mins - rangeStart) / rangeLength);
    }

    protected TimeRepresentation getTimeRepresentation() {
        return session.getDataPlugin().getTimeRepresentation();
    }

    protected SchedulerUIPlugin getUIPlugin() {
        return session.getUIPlugin();
    }

    protected SchedulingSession getSession() { return session; }

    /**
     * Initializes the grid with the given dimensions, but does not paint
     * anything.
     * @param width the width on the screen that the grid should utilize
     * @param height the height on the screen that the grid should utilize
     */
    protected synchronized final void initGrid(int width, int height) {
        this.width = width;
        this.height = height;
        updateDisplayedRange();
    }

    private void updateDisplayedRange() {
        int days;
        if (displayedRange != null) {
            days = displayedRange.getDayCount();
        } else {
            days = 7;
        }
        dayWidth = (width-subx)/((float) days);
        blockHeight = ((float) (height-suby)) / (getLastHourBlock() - getFirstHourBlock() + 1);
    }

    /**
     * Paints the "background" of the time grid - the white rounded rectangle.
     * @param g
     */
    protected synchronized final void paintGridBG(Graphics2D g) {
        // draw grid
        g.setColor(Color.WHITE);
        g.fillRoundRect(margin, margin, width-2*margin, height-2*margin,
                10, 10);
    }

    private static Time getNextHour(Time previous) {
        int hours = previous.getHours() + 1;
        boolean pm = previous.getAmpm() == PM;
        if (hours == 12) pm = !pm;
        if (hours == 13) hours = 1;
        return new Time(hours, 0, pm ? PM : AM);
    }

    /**
     * Paints the "foreground" of the time grid - the grid lines and text
     * headers on rows and columns.
     * @param g the graphics device to paint to
     */
    protected synchronized final void paintGridFG(Graphics2D g) {
        int width = this.width;
        int height = this.height;
        float blockHeight = this.blockHeight;
        float dayWidth = this.dayWidth;

        // more of the grid
        g.setColor(Color.BLACK);
        g.drawRoundRect(margin, margin, width-margin*2, height-margin*2,
                10, 10);

        // print hours along left column
        g.setFont(UITools.getLabelFont().deriveFont(Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();
        Time start = displayedRange.getStart();
        Time end = displayedRange.getEnd();

        Time time;
        if (start.getMinutes() == 0) time = start;
        else time = getNextHour(start);

        while (time.compareTo(end) < 0) {
            int y = getYFromTime(time);

            // draw the hour mark
            g.setColor(Color.BLACK);
            g.setStroke(PLAIN_STROKE);
            String str = Integer.toString(time.getHours());
            g.drawString(str, offx - fm.stringWidth(str) - 1, y + 10);
            g.drawLine(margin, y, width-margin, y);

            if (isHalfHourMarkDrawn()) {
                // draw the half-hour mark
                int ymid = getYFromTime(new Time(time.getHours(), 30, time.getAmpm()));

                g.setColor(Color.LIGHT_GRAY);
                g.setStroke(DASH_STROKE);
                g.drawLine(offx, ymid, width-margin, ymid);
            }

            // move the time to the next hour
            time = getNextHour(time);
        }

        // draw days
        g.setFont(UITools.getLabelFont());
        g.setColor(Color.BLACK);
        g.setStroke(PLAIN_STROKE);
        fm = g.getFontMetrics();
        g.drawLine(margin, offy, width - margin, offy);
        boolean useShortNames = false;
        boolean useLetters = false;
        int maxStringWidth = (int) (dayWidth - 2);
        for (int i = 0; i < DAY_NAMES.length; i++) {
            String dayName = DAY_NAMES[i];
            if (fm.stringWidth(dayName) >= maxStringWidth) {
                useShortNames = true;

                String shortName = DAY_NAMES_SHORT[i];
                if (fm.stringWidth(shortName) > maxStringWidth) {
                    useLetters = true;
                    break;
                }
            }
        }
        int total = 0;
        for (int i = 0; i < 7; i++) {
            if (!displayedRange.isOnDay(i)) continue;

            String dayname;
            if (useShortNames) dayname = DAY_NAMES_SHORT[i];
            else dayname = DAY_NAMES[i];

            if (useLetters) dayname = dayname.substring(0, 1);

            int charWidth = fm.stringWidth(dayname)/2;
            double middle = offx + (dayWidth * (total + 0.5));
            g.drawString(dayname, (int) (middle - charWidth), 15);
            int xpos = (int) getXFromDayNumber(i);
            g.drawLine(xpos, 2, xpos, height-2);

            total++;
        }

        g.drawLine(offx, margin, offx, height - margin);
    }

    protected boolean isHalfHourMarkDrawn() {
        return blockHeight > 12;
    }

    protected abstract TimeGridType getTimeGridType();

    protected Rectangle2D.Float getTimeRect(int day, Time start, Time end) {
        float x = getXFromDayNumber(day);
        int y = getYFromTime(start);
        return new Rectangle2D.Float(x, y,
                getXFromDayNumber(day+1) - x, getYFromTime(end) - y);
    }

    /**
     * Returns a rectangle representing the area of the screen occupied by the
     * given cell in the grid. Note that values of -1 for either
     * {@code xbox} or {@code ybox} return a rectangle containing the
     * entire row or column, respectively.
     * @param column the x-position, or "column" in the grid
     * @param block the y-position, or "row" in the grid
     * @return a rectangle containing the given cell of the grid
     * @see #getGridCoords
     */
    protected synchronized final Rectangle2D.Float getTimeRect(int column,
            int block) {
        float x;
        float y;
        float w;
        float h;
        if (column == -1) {
            x = offx;
            w = width-offx;
        } else {
            x = getXFromColumn(column);
            w = getXFromColumn(column+1)-x;
        }
        if (block == -1) {
            y = offy;
            h = height-offy;
        } else {
            y = getYFromBlockNumber(block);
            h = getYFromBlockNumber(block+1) - y;
        }
        return new Rectangle2D.Float(x, y, w, h);
    }

    private int getYFromBlockNumber(int ybox) {
        return getYFromTime(getTimeRepresentation().getTime(ybox));
    }

    /**
     * Returns the grid coordinates of the given point in the window (i.e., on
     * the screen). Useful for converting mouse coordinates to grid coordinates.
     * @param x the x-position on the screen
     * @param y the y-position on the screen
     * @return a {@code Point} representing an x and y positions of the
     *         grid cell at the given x,y mouse coordinates
     * @see #getTimeRect
     */
    protected synchronized final Point getGridCoords(int x, int y) {
        int colnum;
        if (x < offx) colnum = -1;
        else colnum = (int) ((x - offx) / dayWidth);

        int absBlock;
        if (y < offy) absBlock = -1;
        else absBlock = (int) ((y - offy) / blockHeight);

        int bx;
        if (absBlock < 0) bx = -1;
        else bx = getFirstHourBlock() + absBlock;
        int by;
        if (colnum < 0) by = -1;
        else by = getDayNumberFromColumn(colnum);

        return new Point(by, bx);
    }

    private int getDayNumberFromColumn(int colnum) {
        int sub = 0;
        int i = 0;
        for (boolean on : getDisplayedRange().getDays()) {
            if (i >= colnum) break;
            if (!on) sub++;
            i++;
        }
        return colnum - sub;
    }

    public void setSession(SchedulingSession session) {
        this.session = session;
        if (session != null) {
            initSession();

            Time start = getDisplayedRange().getStart();
            TimeRepresentation rep = getTimeRepresentation();
            firstHourBlock = rep.getClosestBlock(start, EARLIER);
            lastHourBlock = rep.getClosestBlock(getDisplayedRange().getEnd(),
                    LATER) - 1;
            //TOSMALL: make displayed range change with selected schedule
            displayedRange = getUIPlugin().getPreferredTimeGridRange(getTimeGridType());
            updateDisplayedRange();
        }

    }

    public int getLastHourBlock() { return lastHourBlock; }

    public int getFirstHourBlock() { return firstHourBlock; }

    protected void initSession() { }

    public DataContext getDataContext() { return getSession().getDataContext(); }

    public SchedulerEngine getScheduler() { return getSession().getEngine(); }
}
