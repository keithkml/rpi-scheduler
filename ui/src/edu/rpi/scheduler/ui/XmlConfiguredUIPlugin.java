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
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.IntRange;
import edu.rpi.scheduler.schedb.SchedulerTools;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.GradeType;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.ResourceLoader;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchKey;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchType;
import edu.rpi.scheduler.ui.panels.courses.ConflictDetector;
import edu.rpi.scheduler.ui.panels.courses.SelectedCoursesList;
import edu.rpi.scheduler.engine.SelectedCourse;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.MalformedURLException;

public class XmlConfiguredUIPlugin implements SchedulerUIPlugin {
    private static final Logger logger
            = Logger.getLogger(XmlConfiguredUIPlugin.class.getName());
    public static final String SYSPROP_UI_CONFIG_URL = "scheduler.ui.configfile";

    public static final Comparator<Schedule> BY_DAYS_OF_CLASS
            = new Comparator<Schedule>() {
                public int compare(Schedule o1, Schedule o2) {
                    int d1 = o1.getDaysOfClass();
                    int d2 = o2.getDaysOfClass();

                    return d1 > d2 ? 1 : (d1 == d2 ? 0 : -1);
                }
            };
    public static final Comparator<Schedule> BY_CLASS_TIME
            = new Comparator<Schedule>() {
                public int compare(Schedule o1, Schedule o2) {
                    return -compareArrays(o1.getTimeSums(), o2.getTimeSums());
                }
            };
    public static final Comparator<Schedule> BY_TIME_BETWEEN
            = new Comparator<Schedule>() {
                public int compare(Schedule o1, Schedule o2) {
                    int min1 = SchedulerTools.getSmallestBreak(o1);
                    int min2 = SchedulerTools.getSmallestBreak(o2);
                    return min1 < min2 ? -1 : (min1 > min2 ? 1 : 0);
                }
            };

    protected static final String ITEM_HR = "<HR>";

    private static int compareArrays(int[] d1, int[] d2) {
        if (d1.length == 0 || d2.length == 0) {
            return d1.length > 0 ? 1 : (d2.length > 0 ? -1 : 0);
        }

        if (d1[0] > d2[0]) {
            // the first one has longer breaks than the second, so it goes
            // first
            return -1;
        } else if (d1[0] != d2[0]) {
            // the second one has longer breaks than the first.
            return 1;
        }

        int end = Math.min(d1.length, d2.length);
        for (int i = 0; i < end; i++) {
            if (d1[i] != d2[i]) {
                return d1[i] > d2[i] ? -1 : 1;
            }
        }

        // if they're equal up to this point, return the one with the fewest
        // breaks first, I guess.
        return d1.length < d2.length ? -1 : (d1.length == d2.length ? 0 : 1);
    }

    private String adminName = null;
    private String adminEmail = null;
    private String programName = null;
    private DailyTimePeriod range = null;
    private Map<StringFormatType, MessageFormat> stringFormats
            = new HashMap<StringFormatType, MessageFormat>();
    private Map<WindowType, Dimension> windowSizes
            = new HashMap<WindowType, Dimension>();
    private Map<SearchType, Set<SearchKey>> searchTypes
            = new LinkedHashMap<SearchType, Set<SearchKey>>();
    private Map<WindowType, Image> icons = new HashMap<WindowType, Image>();
    private List<Link> links = new ArrayList<Link>();

    private SchedulingSession session;
    private String aboutBoxText = null;

    private List<RankingMethod> rankingMethods = Arrays.asList(
            new RankingMethod("Class time", BY_CLASS_TIME),
            new RankingMethod("Days of class", BY_DAYS_OF_CLASS),
            new RankingMethod("Shortest break", BY_TIME_BETWEEN));

    public XmlConfiguredUIPlugin(SchedulingSession session) {
        this.session = session;
    }

    public void loadSettings() throws PluginLoadingException {
        String urlstr = System.getProperty(SYSPROP_UI_CONFIG_URL);
        if (urlstr == null) {
            throw new PluginLoadingException("No database URL was specified");
        }
        urlstr = getUrlWithCodebase(urlstr);
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(session.getResourceLoader().loadResource(urlstr));
        } catch (IOException e) {
            throw new PluginLoadingException("Couldn't open plugin "
                    + "configuration at " + urlstr, e);
        } catch (JDOMException e) {
            throw new PluginLoadingException("The plugin configuration file is "
                    + "corrupt", e);
        }
        loadFromDocument(doc);

        if (programName == null) programName = "Scheduler";
        if (range == null) {
            range = new DailyTimePeriod(new boolean[]{
                true, true, true, true, true, true, false
            }, new Time(6, 00, Time.AM), new Time(11, 00, Time.PM));
        }
    }

    private String getUrlWithCodebase(String urlstr)
            throws PluginLoadingException {
        URL codebase = session.getCodebase();
        if (codebase != null) {
            try {
                urlstr = new URL(codebase, urlstr).toExternalForm();
            } catch (MalformedURLException e) {
                throw new PluginLoadingException("Invalid database URL string: "
                        + urlstr, e);
            }
        }
        return urlstr;
    }

    private static final Map<String, WindowType> STRING_TO_WINDOW_TYPE
            = new HashMap<String, WindowType>();

    static {
        STRING_TO_WINDOW_TYPE.put("main", WindowType.MAIN);
        STRING_TO_WINDOW_TYPE.put("course-preview", WindowType.COURSE_PREVIEW);
        STRING_TO_WINDOW_TYPE.put("view-schedule", WindowType.VIEW_SCHEDULE);
        STRING_TO_WINDOW_TYPE.put("loading-progress", WindowType.LOADING_PROGRESS);
    }

    private static final Map<String, StringFormatType> STRING_TO_FORMAT_TYPE
            = new HashMap<String, StringFormatType>();

    static {
        STRING_TO_FORMAT_TYPE.put("short-course", StringFormatType.COURSE_SHORT);
        STRING_TO_FORMAT_TYPE.put("very-short-course", StringFormatType.COURSE_VERY_SHORT);
    }

    private void loadFromDocument(Document doc) throws PluginLoadingException {
        Element root = doc.getRootElement();
        Element proginfoel = root.getChild("program-info");
        if (proginfoel != null) {
            programName = proginfoel.getChildTextTrim("program-name");
            Element adminel = proginfoel.getChild("admin");
            if (adminel == null) {
                logger.warning("UI: No admin information present in config file");
            } else {
                adminName = adminel.getAttributeValue("name");
                adminEmail = adminel.getAttributeValue("email");
            }
            aboutBoxText = proginfoel.getChildTextNormalize("about-box-text");
            Element linksel = proginfoel.getChild("help-links");
            if (linksel == null) {
                logger.warning("UI: No Help links present in config file");
            } else {
                List<Element> linkels = linksel.getChildren("help-link");
                for (Element element : linkels) {
                    String href = element.getAttributeValue("href");
                    String name = element.getTextTrim();
                    if (href != null && name.length() > 0) {
                        links.add(new Link(name, href));
                    }
                }
            }
        }
        Element uiinfoel = root.getChild("ui-info");
        if (uiinfoel != null) {
            Element iconsel = uiinfoel.getChild("icons");
            if (iconsel == null) {
                logger.warning("UI: No icons present in config file");
            } else {
                List<Element> iconels = iconsel.getChildren("icon");
                ResourceLoader resourceLoader = session.getResourceLoader();
                for (Element element : iconels) {
                    String type = element.getAttributeValue("type");
                    String href = element.getAttributeValue("href");
                    if (type == null || href == null) {
                        logger.warning("UI: Incomplete icon entry: type='"
                                + type + "', href='" + href + "'");
                    } else {
                        WindowType wintype = STRING_TO_WINDOW_TYPE.get(type);
                        if (wintype == null) {
                            logger.warning("UI: No window type information "
                                    + "present for icon '" + type + "' in "
                                    + "config file");
                        } else {
                            href = getUrlWithCodebase(href);
                            try {
                                InputStream stream = resourceLoader.loadResource(href);
                                icons.put(wintype, ImageIO.read(stream));
                            } catch (Exception ignored) {
                                ignored.printStackTrace();
                            }
                        }
                    }
                }
            }

            Element gridrangeel = uiinfoel.getChild("default-grid-range");
            if (gridrangeel == null) {
                logger.warning("UI: No default grid range present in config file");
            } else {
                boolean[] days;
                String daysstr = gridrangeel.getAttributeValue("days");
                if (daysstr != null) {
                    try {
                        days = readDays(daysstr);
                    } catch (IllegalArgumentException e) {
                        logger.warning("UI: Invalid days list: " + daysstr);
                        days = null;
                    }
                    if (days != null) {
                        String startstr = gridrangeel.getAttributeValue("start-time");
                        String endstr = gridrangeel.getAttributeValue("end-time");
                        Time start = readTime(startstr);
                        Time end = readTime(endstr);
                        if (start == null || end == null) {
                            logger.warning("UI: Incomplete default grid range: days="
                                    + daysstr + ", start=" + startstr + ", end="
                                    + endstr);
                        } else {
                            range = new DailyTimePeriod(days, start, end);
                        }
                    }
                }
            }

            Element windowsizesel = uiinfoel.getChild("window-sizes");
            if (windowsizesel == null) {
                logger.warning("UI: No window sizes present in config file");
            } else {
                List<Element> windowsizeels = windowsizesel.getChildren("window-size");
                for (Element element : windowsizeels) {
                    String typestr = element.getAttributeValue("type");
                    String wstr = element.getAttributeValue("width");
                    String hstr = element.getAttributeValue("height");
                    if (typestr == null || wstr == null || hstr == null) {
                        logger.warning("UI: Incomplete window size: type="
                                + typestr + ", width=" + wstr + ", height="
                                + hstr);
                    } else {
                        try {
                            int width = Integer.parseInt(wstr);
                            int height = Integer.parseInt(hstr);
                            WindowType wintype = STRING_TO_WINDOW_TYPE.get(
                                    typestr);
                            if (wintype == null) {
                                logger.warning("UI: Invalid window type for "
                                        + wstr + "x" + hstr + " window size: "
                                        + typestr);
                            } else {
                                windowSizes.put(wintype,
                                        new Dimension(width, height));
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            Element formatsel = uiinfoel.getChild("display-formats");
            if (formatsel == null) {
                logger.warning("UI: No display formats present in config file");
            } else {
                List<Element> formatels = formatsel.getChildren("display-format");
                for (Element element : formatels) {
                    String type = element.getAttributeValue("type");
                    String fmt = element.getTextTrim();
                    if (type == null) {
                        logger.warning("UI: Incomplete display format entry (no "
                                + "type is present): format=" + fmt);
                    } else {
                        StringFormatType fmttype = STRING_TO_FORMAT_TYPE.get(type);
                        if (fmttype == null) {
                            logger.warning("UI: Invalid display format type '"
                                    + type + "'");
                        } else {
                            MessageFormat mf;
                            try {
                                mf = new MessageFormat(fmt);
                                stringFormats.put(fmttype, mf);
                            } catch (IllegalArgumentException e) {
                                logger.log(Level.WARNING, "UI: Invalid display "
                                        + "format " + fmt, e);
                            }
                        }
                    }
                }
            }
        }
        Element searchtypesel = root.getChild("search-types");
        if (searchtypesel == null) {
            logger.warning("UI: No search types present in config file");
        } else {
            List<Element> searchtypeels = searchtypesel.getChildren("search-type");
            for (Element stel : searchtypeels) {
                String name = stel.getAttributeValue("name");
                Set<SearchKey> keys = new LinkedHashSet<SearchKey>();
                List<Element> searchkeyels = stel.getChildren("search-key");
                for (Element skel : searchkeyels) {
                    String skname = skel.getAttributeValue("name");
                    if (skname == null) {
                        logger.warning("UI: Search key without a name for "
                                + "type '" + name + "'");
                    } else {
                        keys.add(new SearchKey(skname));
                    }
                }

                if (name == null || keys.isEmpty()) {
                    logger.warning("UI: Incomplete search type entry, name='"
                            + name + "', keys=" + keys);
                } else {
                    searchTypes.put(new SearchType(name), keys);
                }
            }
        }
    }

    protected static boolean[] readDays(String daysstr)
            throws IllegalArgumentException {
        boolean[] days = new boolean[7];
        // this regular expression allows things like "wed-thur-fri",
        // "wednesday thursday friday", "wed/thu/fri", or the preferred
        // "wed,thu,fri"
        int valid = 0;
        String[] daystrs = daysstr.split("[,-/+ ]+");
        for (final String newVar : daystrs) {
            String daystr = newVar.toLowerCase().trim();
            if (daystr.length() == 0) continue;

            // this matches thing like:
            //   "mon" for monday
            //   "t" for tuesday
            //   "tr" for thursday
            //   "thur" for thursday
            //   "su" for sunday
            // note that "s" doesn't match anything, neither saturday nor sunday
            if ("monday".startsWith(daystr)) {
                days[0] = true;
            } else if ("tuesday".startsWith(daystr)) {
                days[1] = true;
            } else if ("wednesday".startsWith(daystr)) {
                days[2] = true;
            } else if ("thursday".startsWith(daystr)
                    || daystr.equals("tr") || daystr.equals("r")) {
                days[3] = true;
            } else if ("friday".startsWith(daystr)) {
                days[4] = true;
            } else if (daystr.length() >= 2 && "saturday".startsWith(daystr)) {
                days[5] = true;
            } else if (daystr.length() >= 2 && "sunday".startsWith(daystr)) {
                days[6] = true;
            }
            valid++;
        }

        if (valid == 0 && !daysstr.trim().equals("")) {
            throw new IllegalArgumentException("no day names found in string '"
                    + daysstr + "'");
        }

        return days;
    }

    protected static final Pattern timeRE = Pattern.compile(
            "(\\d{1,2}):(\\d{2})\\s?(AM|PM)", Pattern.CASE_INSENSITIVE);

    protected static Time readTime(String timestr) {
        if (timestr == null) return null;

        String ntimestr = timestr.trim();
        Matcher m = timeRE.matcher(ntimestr);
        if (!m.matches()) {
            return null;
        }

        String hstr = m.group(1);
        String mstr = m.group(2);
        String ampm = m.group(3);
        int hi;
        int mi;
        try {
            hi = Integer.parseInt(hstr);
            mi = Integer.parseInt(mstr);
        } catch (NumberFormatException e) {
            return null;
        }
        if (hi < 1 || hi > 12) {
            return null;
        }
        if (mi < 0 || mi > 59) {
            return null;
        }
        boolean pm = ampm.equalsIgnoreCase("pm");

        return new Time(hi, mi, pm ? Time.PM : Time.AM);
    }

    public Image getWindowIcon(WindowType type) {
        Image image = icons.get(type);
        if (image == null) return null;

        return image;
    }

    public Map<SearchType, Set<SearchKey>> getCourseSearchTypes() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<SearchType, Set<SearchKey>>(searchTypes));
    }

    public String getAdminName() { return adminName; }

    public String getAdminEmail() { return adminEmail; }

    public String getProgramName() { return programName; }

    public synchronized String getCourseString(CourseDescriptor cd,
            StringFormatType type) {
        MessageFormat mf = stringFormats.get(type);
        if (mf == null) return cd.toString();

        Course course = cd.getActualCourse();
        Department dept = cd.getDept();
        StringBuffer result = new StringBuffer(50);
        mf.format(new Object[]{
            /* 0 */ dept.getAbbrev(),
            /* 1 */ dept.getName(),
            /* 2 */ course.getNumber().toString(),
            /* 3 */ course.getName(),
            /* 4 */ course.getCredits().getFrom(),
            /* 5 */ course.getCredits().getTo(),
            /* 6 */ course.getCredits().toString(),
            /* 7 */ course.getSections().size()
        }, result, null);
        return result.toString();
    }

    public DailyTimePeriod getPreferredTimeGridRange(TimeGridType type) {
        return range;
    }

    public Dimension getDefaultWindowSize(WindowType type) {
        return windowSizes.get(type);
    }

    public String getAboutBoxText() {
        return aboutBoxText;
    }

    public Collection<Link> getLinks() {
        return links;
    }

    public String getInfoText(CourseDescriptor cd,
            SelectedCoursesList selectedCourses,
            ConflictDetector conflictDetector) {
        String text;
        if (cd == null) {
            text = "<font style=\"font-size: small\">"
                    + "No course selected</font>";
        } else {
            Course course = cd.getActualCourse();
            List<String> items = new ArrayList<String>();
            items.add("<B>" + course.getName() + "</B>");
            items.add(ITEM_HR);
            String taughtby = UITools.getTaughtByString(cd);
            if (taughtby != null) items.add("Professors: " + taughtby);

            Collection<Section> sections = course.getSections();

            items.add("Sections: " + sections.size());

            IntRange credits = course.getCredits();
            if (credits != null && credits.getTo() > 0) {
                items.add("Credits: " + credits.toString());
            }
            GradeType grade = course.getGradeType();
            if (grade != null) {
                items.add("Grade type: " + grade.getName());
            }

            int seats = 0;
            for (Section section : sections) {
                int s = section.getSeats();
                if (s != -1) seats += s;
            }
            if (seats > 0) items.add("Total seats in course: " + seats);


            String classTypes = UITools.getClassTypesString(cd);
            if (classTypes != null) items.add("Classes: " + classTypes);

            Notes notes = course.getNotes();
            if (notes != null) {
                List<String> noteList = notes.getNotesAsText();
                if (!noteList.isEmpty()) {
                    StringBuffer notessb = new StringBuffer(100);
                    notessb.append("<BR>Course Notes:<UL style=\"margin-left:13px; "
                            + "font-size: small; margin-right: 0px\">");
                    for (String note : noteList) {
                        notessb.append("<LI style=\"font-size: small\">" + note + "</LI>");
                    }
                    notessb.append("</UL>");
                    items.add(notessb.toString());
                }
            }

            if (classTypes == null) {
                items.add(ITEM_HR);
                items.add("<B>This class has no registered class time.</B>");
            }

            if (!selectedCourses.containsCourse(cd)) {
                addConflictText(cd, items, conflictDetector);
            }

            text = getTextFromItems(items);
        }
        return text;
    }

    public List<RankingMethod> getRankingMethods() {
        return rankingMethods;
    }

    protected void addConflictText(CourseDescriptor cd, List<String> items,
            ConflictDetector conflictDetector) {
        Collection<SelectedCourse> conflicts = conflictDetector.getSelectedConflicts(cd);
        if (!conflicts.isEmpty()) {
            items.add(ITEM_HR);
            String fontTag = "<div style=\"background-color: #ff6666; padding: 4px\">";
            if (conflicts.size() == 1) {
                CourseDescriptor conflicted = conflicts.iterator().next().getCourse();
                items.add(fontTag + "Conflicts with " + conflicted.getActualCourse().getName() + "</div>");

            } else {
                StringBuffer buf = new StringBuffer();
                buf.append(fontTag);
                buf.append("Conflicts with: <UL style=\"margin-left:13px; "
                        + "margin-right: 0px; margin-bottom: 0px;\">");
                for (SelectedCourse conflict : conflicts) {
                    Course conflictedCourse = conflict.getCourse().getActualCourse();
                    buf.append("<li style=\"font-size: small\"> " + conflictedCourse.getName() + "</li>");
                }
                buf.append("</ul></div>");
                items.add(buf.toString());
            }

        } else if (!conflictDetector.wouldFitAnySchedule(cd)) {
            // there's no specific course that conflicts, but this course
            // wouldn't fit in any schedule
            items.add(ITEM_HR);
            items.add("This course would not fit in any schedule with the "
                    + "courses you have selected.");
        }
    }

    protected String getTextFromItems(List<String> items) {
        String text;
        StringBuffer buf = new StringBuffer();
        buf.append("<HTML>");
        for (Iterator<String> it = items.iterator(); it.hasNext();) {
            String s = it.next();
            buf.append(s);
            if (!s.equals(ITEM_HR) && it.hasNext()) buf.append("<BR>");
        }

        text = buf.toString();
        return text;
    }
}
