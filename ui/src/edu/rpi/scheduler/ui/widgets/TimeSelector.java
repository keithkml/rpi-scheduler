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

import edu.rpi.scheduler.schedb.WeekMask;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.DayMask;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.TimeGridType;
import edu.rpi.scheduler.CopyOnWriteArrayList;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * A component allowing the user to block out times interactively on a grid.
 */
public class TimeSelector extends TimeGrid {
    public static final String PROP_TIMEMASK = "timeMask";
    
    private static final Color COLOR_SELON = new Color(0, 0, 128);
    private static final Color COLOR_SELOFF = new Color(128, 0, 0);
    private static final Color COLOR_HOVER = new Color(192, 192, 192);

    {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getY() >= getGridHeight() - 2) return;

                setStartSel(e);
                setEndSel(e);
                selOn = !isTimeBlockOn(startSel.x, startSel.y)
                        && (e.getModifiers()
                        & (MouseEvent.BUTTON1_MASK
                        | MouseEvent.CTRL_MASK)) != 0;
            }

            public void mouseReleased(MouseEvent e) {
                Point startSel = TimeSelector.this.startSel;
                Point endSel = TimeSelector.this.endSel;
                if (startSel == null) return;
                setEndSel(e);
                int x1 = Math.min(startSel.x, endSel.x);
                int x2 = Math.max(startSel.x, endSel.x);
                int y1 = Math.min(startSel.y, endSel.y);
                int y2 = Math.max(startSel.y, endSel.y);

                int mods = e.getModifiers();
                boolean delete = (mods & MouseEvent.CTRL_MASK) != 0
                        || (mods & MouseEvent.BUTTON1_MASK) == 0
                        || isTimeBlockOn(startSel.x, startSel.y);
                for (int i = x1; i <= x2; i++) {
                    for (int j = y1; j <= y2; j++) {
                        selBox(i, j, delete);
                    }
                }
                clearSel();
            }

            public void mouseExited(MouseEvent e) {
                parseHover(-1, -1);
            }
        });

        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                setEndSel(e);
                parseHover(-1, -1);
            }

            public void mouseMoved(MouseEvent e) {
                if (e.getY() >= getGridHeight() - 2) {
                    parseHover(-1, -1);
                    return;
                }

                parseHover(e.getX(), e.getY());
            }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    private boolean isTimeBlockOn(int day, int block) {
        boolean noday = day < 0;
        boolean noblock = block < 0;
        int maxBlock = timeMask.getDayMask(0).getMaxBlockNum();
        if (noday && noblock || day > 7 || block > maxBlock) return false;
        if (noday) {
            for (int di = 0; di < 7; di++) {
                if (!timeMask.isOn(di, block)) return false;
            }
            return true;
        } else if (noblock) {
            for (int bi = 0; bi <= maxBlock; bi++) {
                if (!timeMask.isOn(day, bi)) return false;
            }
            return true;
        } else {
            return timeMask.isOn(day, block);
        }
    }

    private boolean selOK = false;
    private boolean selOn = false;
    private final NumberFormat format = NumberFormat.getNumberInstance();

    {
        format.setMinimumIntegerDigits(2);
    }

    private WeekMask<?> timeMask = null;

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public TimeSelector() {
    }

    public TimeSelector(SchedulingSession session) {
        super(session);
    }

    public void setSession(SchedulingSession session) {
        super.setSession(session);

        if (session != null) {
            clearTimeMask();
        } else {
            timeMask = null;
        }
    }

    public void clearTimeMask() {
        SchedulerDataPlugin plugin = getSession().getDataPlugin();
        setTimeMask(plugin.getTimeRepresentation().newWeekMask());
    }

    private Rectangle getSelRect() {
        Point startSel = this.startSel;
        Point endSel = this.endSel;

        int x1 = Math.min(startSel.x, endSel.x);
        int y1 = Math.min(startSel.y, endSel.y);
        int x2 = Math.max(startSel.x, endSel.x);
        int y2 = Math.max(startSel.y, endSel.y);

        Rectangle2D.Float rect1 = getTimeRect(x1, y1);
        Rectangle2D.Float rect2 = getTimeRect(x2, y2);

        return rect1.createUnion(rect2).getBounds();
    }

    private void paintSel() {
        Point startSel = this.startSel;
        Point endSel = this.endSel;

        boolean badSel = (startSel == null || this.endSel == null
                        || (startSel.x == -1 && startSel.y == -1)
                        || (endSel.x == -1 && endSel.y == -1));
        boolean selOK = !badSel;
        this.selOK = selOK;
        if (!selOK) return;

        repaint(getSelRect());
    }

    private Point getSel(MouseEvent e) {
        return getGridCoords(e.getX(), e.getY());
    }

    private boolean sameSel(Point p, MouseEvent e) {
        return p.equals(getSel(e));
    }

    private void clearSel() {
        if (selOK) paintSel();
        startSel = null;
        endSel = null;
        if (selOK) paintSel();
    }

    private void setStartSel(MouseEvent e) {
        if (selOK && sameSel(startSel, e)) return;
        paintSel();
        startSel = getSel(e);
        paintSel();
    }

    private void setEndSel(MouseEvent e) {
        if (selOK && sameSel(startSel, e)) return;
        paintSel();
        endSel = getSel(e);
        paintSel();
    }

    private int lastx = -50;
    private int lasty = -50;
    private boolean lastdel;
    private Point hover = null;
    private Point startSel = null;
    private Point endSel = null;

    private void parseHover(int x, int y) {
        Point coords = getGridCoords(x, y);
        Point hover = this.hover;
        if (hover != null && coords.equals(hover)) return;

        // repaint where the old selection was
        if (hover != null) repaint(getTimeRect(hover.x, hover.y).getBounds());

        this.hover = coords;
        hover = coords;

        // and the new selection
        repaint(getTimeRect(hover.x, hover.y).getBounds());
    }

    private void selBox(int xbox, int ybox, boolean delete) {
        if (delete == lastdel && xbox == lastx && ybox == lasty) return;
        WeekMask<?> timeMask = this.timeMask;
        if (timeMask == null) return;

        lastdel = delete;
        lastx = xbox;
        lasty = ybox;
        boolean repaint = false;
        if ((xbox >= 0) && (ybox >= 0)) {
            // the user clicked a specific box
            if (delete) {
                repaint = timeMask.delete(xbox, ybox);
            } else {
                repaint = timeMask.add(xbox, ybox);
            }
        } else if ((xbox == -1) && (ybox >= 0)) {
            // the user clicked one of the hours on the left
            if (delete) {
                for (int day = 0; day < 7; day++) {
                    repaint = repaint | timeMask.delete(day, ybox);
                }
            } else {
                for (int day = 0; day < 7; day++) {
                    repaint = repaint | timeMask.add(day, ybox);
                }
            }
        } else if ((ybox == -1) && (xbox >= 0) && (xbox < 7)) {
            // the user clicked one of the days at the top
            if (delete) {
                repaint = timeMask.clear(xbox);
            } else {
                repaint = timeMask.fill(xbox);
            }
        }
        if (repaint) {
            repaint(getTimeRect(xbox, ybox).getBounds());
            pcs.firePropertyChange(PROP_TIMEMASK, null, timeMask);
        }
    }

    public final void paintComponent(Graphics og) {
        super.paintComponent(og);

        Graphics2D g = (Graphics2D) og;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        initGrid(getWidth(), getHeight());

        paintGridBG(g);

        Rectangle2D.Float hoverRect = null;
        Point hover = this.hover;
        if (hover != null && (hover.x != -1 || hover.y != -1)) {
            hoverRect = getTimeRect(hover.x, hover.y);

//            g.setColor(new Color(200, 200, 255));
//            System.out.println("filling " + hoverRect);
//            g.fill(hoverRect);
        }

        // draw filled sections
        g.setPaint(Color.GRAY);
        WeekMask<?> mask = this.timeMask;
        if (mask != null) {
            int firstBlock = getFirstHourBlock();
            int lastBlock = getLastHourBlock();
            DailyTimePeriod displayedRange = getDisplayedRange();
            for (int day = 0; day < 7; day++) {
                if (mask.isEmpty(day) || !displayedRange.isOnDay(day)) continue;
                DayMask dm = mask.getDayMask(day);

                int col = getColumnFromDayNumber(day);
                int maxBlockNum = dm.getMaxBlockNum();
                for (int block = firstBlock;
                     block < maxBlockNum && block <= lastBlock; block++) {
                    if (dm.isOn(block)) g.fill(getTimeRect(col, block));
                }
            }
        }

        if (selOK) {
            Color color;
            if (selOn) color = COLOR_SELON;
            else color = COLOR_SELOFF;
            g.setColor(color);
            Rectangle rect = getSelRect();
            g.fill(rect);
        } else

        if (hoverRect != null) {
            g.setColor(COLOR_HOVER);
            g.fill(hoverRect);
        }

        paintGridFG(g);
    }

    /**
     * Sets the blocked time mask to be displayed and edited by the user.
     * @param mask the blocked time mask to be edited
     */
    public void setTimeMask(WeekMask<?> mask) {
        this.timeMask = mask;

        pcs.firePropertyChange(PROP_TIMEMASK, null, mask);

        repaint();
    }

    /**
     * Returns the current time mask, which may or may not have been edited by
     * the user.
     * @return the time mask being displayed and edited
     */
    public WeekMask<?> getTimeMask() { return timeMask; }

    protected TimeGridType getTimeGridType() {
        return TimeGridType.TIME_SELECTOR;
    }

    public void addTimeMaskChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removeTimeMaskChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}