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

package edu.rpi.scheduler.ui;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.Duration;
import edu.rpi.scheduler.schedb.IntRange;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Location;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.PeriodType;
import edu.rpi.scheduler.schedb.spec.Professor;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.savesched.LoadedSchedule;
import edu.rpi.scheduler.ui.savesched.SchedulePersister;
import edu.rpi.scheduler.ui.savesched.DocumentFormatException;
import edu.rpi.scheduler.ui.print.SchedulePrinter;

import javax.swing.UIManager;
import javax.swing.JTextPane;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.Document;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Window;
import java.awt.Font;
import java.awt.Component;
import java.awt.print.PrinterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class UITools {
    private static final Logger logger
            = Logger.getLogger(UITools.class.getName());

    private static final String[] DAYNAMES = {
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };
    private static final String PROP_WIDTH = "width";
    private static final String PROP_HEIGHT = "height";
    private static final String PROP_X = "x";
    private static final String PROP_Y = "y";

    private static final String HOME = System.getProperty("user.home");

    private UITools() { }

    public static Duration getDuration(Collection<ClassPeriod> periods) {
        return new Duration(getTotalMinutes(periods));
    }

    public static int getTotalMinutes(Collection<ClassPeriod> periods) {
        int timeSum = 0;
        for (ClassPeriod period : periods) {
            DailyTimePeriod timePeriod = period.getTimePeriod();
            int mins = timePeriod.getDuration().getTotalMinutes();
            for (boolean on : timePeriod.getDays()) {
                if (on) timeSum += mins;
            }
        }
        return timeSum;
    }

    public static String getDaysString(DailyTimePeriod period) {
        StringBuffer sb = new StringBuffer(10);
        int day = 0;
        for (boolean on : period.getDays()) {
            if (on) {
                if (sb.length() != 0) sb.append(",");
                sb.append(DAYNAMES[day]);
            }
            day++;
        }
        return sb.toString();
    }

    public static String getTimeRangeText(DailyTimePeriod timePeriod) {
        Time start = timePeriod.getStart();
        Time end = timePeriod.getEnd();

        boolean sameAmpm = start.getAmpm() == end.getAmpm();
        return getTimeStringNumberPart(start)
                + (sameAmpm ? "" : getAmpmString(start) + " ")
                + " - "
                + getTimeStringNumberPart(end) + " "
                + getAmpmString(end);
    }

    private static String getAmpmString(Time end) {
        return end.getAmpm() == Time.PM ? "PM" : "AM";
    }

    private static String getTimeStringNumberPart(Time start) {
        return start.getHours() + ":" + getMinutesString(start);
    }

    private static String getMinutesString(Time start) {
        String minstr = Integer.toString(start.getMinutes());
        while (minstr.length() < 2) minstr = '0' + minstr;
        return minstr;
    }

    public static String getSectionInfoText(SectionDescriptor sd) {
        if (sd == null) return "";

        Section section = sd.getActualSection();
        Collection<ClassPeriod> periods = section.getPeriods();
        Duration timePerWeek = UITools.getDuration(periods);
        String prefix = "<B>Section " + section.getNumber()
                + " (" + section.getID() + ")</B>"
                + "<HR>"
                + "Class time: " + timePerWeek + " per week<BR>"
                + "Seats: " + section.getSeats();
        StringBuffer buf = new StringBuffer(prefix);
        if (periods != null) {
            buf.append("<BR><BR>"
                    + "Class times:<BR>"
                    + "<UL style=\"margin-left:13px; "
                    + "margin-right: 0px\"> ");
            for (ClassPeriod period : periods) {
                DailyTimePeriod timePeriod = period.getTimePeriod();
                buf.append("<LI style=\"font-size: small\">" + period.getType().getName() + "<BR>"
                        + UITools.getTimeRangeText(timePeriod) + " on "
                        + UITools.getDaysString(timePeriod));
                Location location = period.getLocation();
                if (location != null) buf.append("<BR>Location: " + location);
                Professor professor = period.getProfessor();
                if (professor != null) buf.append("<BR>Professor: " + professor.getName());
                buf.append("</LI>");
            }
            buf.append("</UL>");
        }
        Notes notes = section.getNotes();
        if (notes != null) {
            List<String> noteList = notes.getNotesAsText();
            if (!noteList.isEmpty()) {
                buf.append("<BR>"
                        + "<BR>"
                        + "Notes: <UL style=\"margin-left:13px; "
                        + "margin-right: 0px\">");
                for (String note : noteList) {
                    buf.append("<LI style=\"font-size: small\">" + note + "</LI>");
                }
                buf.append("</UL>");
            }
        }
        return buf.toString();
    }

    public static String getClassTypesString(CourseDescriptor coursedesc) {
        Collection<Section> sections = coursedesc.getActualCourse().getSections();

        SortedMap<PeriodType,Integer> mins = new TreeMap<PeriodType, Integer>();
        SortedMap<PeriodType,Integer> maxes = new TreeMap<PeriodType, Integer>();
        for (Section section : sections) {
            Collection<ClassPeriod> periods = section.getPeriods();
            Map<PeriodType,Integer> types = new HashMap<PeriodType, Integer>();
            for (ClassPeriod period : periods) {
                DailyTimePeriod tperiod = period.getTimePeriod();
                PeriodType type = period.getType();

                if (type != null) {
                    int dayc = 0;
                    boolean[] days = tperiod.getDays();
                    for (final boolean newVar : days) {
                        if (newVar) dayc++;
                    }

                    incrValue(types, type, dayc);
                }
            }

            for (Map.Entry<PeriodType,Integer> entry : types.entrySet()) {
                PeriodType key = entry.getKey();
                int typesVal = entry.getValue();

                Integer minsVal = mins.get(key);
                int min;
                if (minsVal == null) {
                    min = typesVal;
                } else {
                    min = Math.min(minsVal, typesVal);
                }
                mins.put(key, min);

                Integer maxesVal = maxes.get(key);
                int max;
                if (maxesVal == null) {
                    max = typesVal;
                } else {
                    max = Math.max(maxesVal, typesVal);
                }
                maxes.put(key, max);
            }
        }

        if (mins.isEmpty()) return null;

        List<String> strings = new LinkedList<String>();
        for (PeriodType key : mins.keySet()) {
            int min = mins.get(key);
            int max = maxes.get(key);
            IntRange range = new IntRange(min, max);
            strings.add(range.toString() + ' ' + key.getName().toLowerCase());
        }
        StringBuffer buf = new StringBuffer();
        listify(buf, strings);
        buf.append(" per week");
        return buf.toString();
    }

    public static String listify(Collection<?> things) {
        StringBuffer buffer = new StringBuffer();
        listify(buffer, things);
        return buffer.toString();
    }

    public static void listify(StringBuffer sb, Collection<?> things) {
        int size = things.size();
        Iterator it = things.iterator();
        if (size == 1) {
            sb.append(it.next());
        } else if (size == 2) {
            sb.append(it.next());
            sb.append(" and ");
            sb.append(it.next());
        } else {
            boolean first = true;
            while (it.hasNext()) {
                Object thing = it.next();

                if (!first && !it.hasNext()) sb.append(" and ");
                sb.append(thing);
                if (it.hasNext()) sb.append(", ");

                first = false;
            }
        }
    }

    private static <K> void incrValue(Map<K,Integer> map, K key, int add) {
        Integer integer = map.get(key);
        int val;
        if (integer == null) val = add;
        else val = integer + add;
        map.put(key, val);
    }

    public static String getTaughtByString(CourseDescriptor coursedesc) {
        Collection<Section> sections = coursedesc.getActualCourse().getSections();
        SortedSet<String> profs = new TreeSet<String>();
        for (Section section : sections) {
            Collection<ClassPeriod> periods = section.getPeriods();
            for (ClassPeriod period : periods) {
                Professor prof = period.getProfessor();
                if (prof != null) profs.add(prof.getName());
            }
        }

        if (profs.isEmpty()) return null;

        StringBuffer sb = new StringBuffer(30);
        listify(sb, profs);
        return sb.toString();
    }

    public static boolean hasClassTime(Course course) {
        for (Section section : course.getSections()) {
            for (ClassPeriod period : section.getPeriods()) {
                if (period.getTimePeriod().getDuration().getTotalMinutes() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean loadWindowPosition(Window window, Preferences prefs,
            String key) {
        Preferences subnode = prefs.node(key);
        List<String> keys;
        try {
            keys = Arrays.asList(subnode.keys());
        } catch (BackingStoreException e) {
            return false;
        }
        if (keys.contains(PROP_X) && keys.contains(PROP_Y)) {
            int x = subnode.getInt(PROP_X, 0);
            int y = subnode.getInt(PROP_Y, 0);
            window.setLocation(x, y);
            return true;
        } else {
            return false;
        }
    }

    public static boolean loadWindowSize(Window window, Preferences prefs,
            String key) {
        Preferences subnode = prefs.node(key);
        List<String> keys;
        try {
            keys = Arrays.asList(subnode.keys());
        } catch (BackingStoreException e) {
            return false;
        }
        if (keys.contains(PROP_WIDTH) && keys.contains(PROP_HEIGHT)) {
            int w = subnode.getInt(PROP_WIDTH, 300);
            int h = subnode.getInt(PROP_HEIGHT, 200);
            window.setSize(w, h);
            return true;
        } else {
            return false;
        }
    }

    public static void fixWindowLocation(Window window) {
        Point location = window.getLocation();
        if (location.x < 0) location.x = 0;
        if (location.y < 0) location.y = 0;
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        DisplayMode mode = gc.getDevice().getDisplayMode();
        if (location.x > mode.getWidth() - 5) location.x = mode.getWidth();
        if (location.y > mode.getHeight() - 25) location.y = mode.getHeight();
        window.setLocation(location.x, location.y);
    }

    public static void fixWindowSize(Window window) {
        Point location = window.getLocation();
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        DisplayMode mode = gc.getDevice().getDisplayMode();
        Dimension size = window.getSize();
        if (location.x + size.width > mode.getWidth()) {
            size.width = mode.getWidth() - location.x - 5;
        }
        if (location.y + size.height > mode.getHeight()) {
            size.height = mode.getHeight() - location.y - 5;
        }
        window.setSize(size);
    }

    public static void saveWindowPositionAndSize(Window window,
            Preferences prefs, String key) {
        Preferences subkey = prefs.node(key);
        Point location = window.getLocation();
        subkey.putInt(PROP_X, location.x);
        subkey.putInt(PROP_Y, location.y);
        Dimension size = window.getSize();
        subkey.putInt(PROP_WIDTH, size.width);
        subkey.putInt(PROP_HEIGHT, size.height);
        try {
            subkey.flush();
        } catch (BackingStoreException alright) { }
    }

    public static String getSectionNumberList(Collection<SectionDescriptor> sections) {
        StringBuffer sb = new StringBuffer(10);
        getSectionNumberList(sections, sb);
        return sb.toString();
    }

    public static void getSectionNumberList(
            Collection<SectionDescriptor> sections, StringBuffer buf) {
        int size = sections.size();
        if (size == 1) {
            SectionDescriptor first = sections.iterator().next();
            buf.append(first.getActualSection().getNumber());

        } else if (size == 2) {
            Iterator<SectionDescriptor> iterator = sections.iterator();
            SectionDescriptor first = iterator.next();
            SectionDescriptor second = iterator.next();
            buf.append(first.getActualSection().getNumber()
                    + " and " + second.getActualSection().getNumber());

        } else {
            boolean first = true;
            for (Iterator<SectionDescriptor> it = sections.iterator();
                 it.hasNext();) {
                SectionDescriptor sd = it.next();
                if (first) first = false;
                else buf.append(", ");
                if (!it.hasNext()) buf.append("and ");
                buf.append(sd.getActualSection().getNumber());
            }
        }
    }

    public static Font getLabelFont() {
        return UIManager.getFont("Label.font");
    }

    public static void makeTextPaneLookLikeDialog(JTextPane pane) {
        Document doc = pane.getDocument();
        Font font = UIManager.getFont("Label.font");
        if (font == null) font = new Font("dialog", Font.PLAIN, 12);
        pane.setFont(font);
        if (doc instanceof HTMLDocument) {
            StyleSheet ss = ((HTMLDocument) doc).getStyleSheet();
            ss.addRule("body { font-family: " + font.getName() + "; "
                    + "font-size: " + font.getSize() + "pt; "
                    + "font-weight: " + (font.isBold() ? "bold" : "inherit") + ";"
                    + "font-style: " +  "; }");
        }
        pane.setOpaque(false);
    }

    public static LoadedSchedule loadSchedule(Window parent,
            SchedulingSession session, File file) {
        SchedulePersister sp = new SchedulePersister();
        LoadedSchedule scheduleInfo;
        try {
            FileInputStream in = new FileInputStream(file);
            scheduleInfo = sp.loadSchedule(session, in);
            in.close();

        } catch (IOException e1) {
            logger.log(Level.WARNING, "Couldn't open file "
                    + file.getPath(), e1);
            JOptionPane.showMessageDialog(parent, "The "
                    + "schedule you chose could not be loaded because of "
                    + "an error opening the file.", "Open Schedule Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;

        } catch (DocumentFormatException e1) {
            logger.log(Level.WARNING, "Couldn't open file "
                    + file.getPath(), e1);
            JOptionPane.showMessageDialog(parent, "The "
                    + "schedule you chose could not be loaded. The file "
                    + "may be corrupt.\n\n(Details: " + e1.getMessage()
                    + ")",
                    "Open Schedule Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return scheduleInfo;
    }

    public static boolean printSchedule(Component parent, SchedulePrinter printer) {
        try {
            printer.print();
            return true;
        } catch (PrinterException e) {
            logger.log(Level.WARNING, "Error while printing", e);
            JOptionPane.showMessageDialog(parent, "The schedule you selected "
                    + "could not be printed due to an internal error in your "
                    + "computer's printing facilities. Try again later.",
                    "Print Schedule Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static File getSchedulerConfigFolder() {
        File homeFolder = new File(HOME);
        File schedulerFolder = new File(homeFolder, ".Scheduler");
        if (!schedulerFolder.isDirectory()) schedulerFolder.mkdir();
        return schedulerFolder;
    }
}
