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

package edu.rpi.scheduler.ui.savesched;

import edu.rpi.scheduler.schedb.DefaultSchedule;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.WeekMask;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.SchedulerData;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.SectionID;
import edu.rpi.scheduler.ui.SchedulingSession;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SchedulePersister {
    private static final Logger logger
            = Logger.getLogger(SchedulePersister.class.getName());

    private static final String SAVED_SCHEDULE_CHARSET = "UTF-8";
    private static final String CURRENT_FILE_VERSION = "4.0";
    private static final Pattern VERSION_PATTERN
            = Pattern.compile("(\\d+)\\.(\\d+)?");

    private static boolean isVersionLaterThanCurrent(String test) {
        Matcher tm = VERSION_PATTERN.matcher(test);
        if (!tm.matches()) return true;
        int tmaj = Integer.parseInt(tm.group(1));
        int tmin = Integer.parseInt(tm.group(2));

        Matcher cm = VERSION_PATTERN.matcher(CURRENT_FILE_VERSION);
        if (!cm.matches()) {
            logger.severe(CURRENT_FILE_VERSION
                    + " does not match " + VERSION_PATTERN.pattern());
            return true;
        }
        int cmaj = Integer.parseInt(cm.group(1));
        int cmin = Integer.parseInt(cm.group(2));

        return tmaj > cmaj || tmin > cmin;
    }

    public LoadedSchedule loadSchedule(SchedulingSession session, InputStream in)
            throws IOException, DocumentFormatException {
        Reader reader = new InputStreamReader(in, SAVED_SCHEDULE_CHARSET);
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(reader);
        } catch (JDOMException e) {
            throw new DocumentFormatException("The file is corrupt", e);
        }

        SchedulerData schedulerData = session.getEngine().getSchedulerData();
        List<UniqueSection> uses = new ArrayList<UniqueSection>(10);

        Element root = document.getRootElement();
        if (!root.getName().equals("saved-schedule")) {
            throw new DocumentFormatException("This file does not appear to "
                    + "contain a saved schedule");
        }
        String version = root.getAttributeValue("version");
        if (version != null && isVersionLaterThanCurrent(version)) {

        }
        Element sectionsel = root.getChild("sections");
        int skipped = 0;
        for (Object usobj : sectionsel.getChildren("unique-section")) {
            Element uniqueel = (Element) usobj;
            String deptAbbrev = uniqueel.getAttributeValue("dept-abbrev");
            String courseId = uniqueel.getAttributeValue("course-id");
            if (deptAbbrev == null || courseId == null) {
                skipped++;
                continue;
            }

            Department dept = schedulerData.getDepartment(deptAbbrev);
            if (dept == null) {
                skipped++;
                continue;
            }
            Course course = dept.getCourseWithCourseIdString(courseId);
            if (course == null) {
                skipped++;
                continue;
            }

            List<Section> sections = new ArrayList<Section>(10);
            WeekMask<?> mask = null;

            for (Object sobj : uniqueel.getChildren("section")) {
                Element sectionel = (Element) sobj;
                String idstr = sectionel.getAttributeValue("id");
                if (idstr == null) {
                    skipped++;
                    continue;
                }

                Section section = course.getSectionWithSectionIdString(idstr);
                if (section == null) {
                    skipped++;
                    continue;
                }

                sections.add(section);

                // make sure the week masks line up
                WeekMask<?> thismask = section.getWeekMask();
                if (mask == null) mask = thismask;
                else if (!mask.equals(thismask)) {
                    skipped++;
                    continue;
                }
            }

            if (sections.isEmpty()) {
                skipped++;
                continue;
            }

            UniqueSection us = new UniqueSection(mask);
            for (Section section : sections) {
                us.addSection(new SectionDescriptor(dept, course, section));
            }

            uses.add(us);
        }

        if (uses.isEmpty()) {
            throw new DocumentFormatException("Could not find any schedule "
                    + "information in the file");
        }

        Schedule sched = new DefaultSchedule(session.getDataPlugin());
        for (UniqueSection us : uses) sched.addSection(us);

        return new LoadedSchedule(sched, skipped);
    }

    public void saveSchedule(SchedulingSession session, Schedule sched,
            OutputStream out) throws IOException {
        Document doc = new Document();
        Element root = new Element("saved-schedule");
        doc.setRootElement(root);
        root.setAttribute("version", CURRENT_FILE_VERSION);
        Element sectionsel = new Element("sections");
        root.addContent(sectionsel);
        for (UniqueSection us : sched.getSections()) {
            Element uniqueel = new Element("unique-section");
            sectionsel.addContent(uniqueel);

            uniqueel.setAttribute("dept-abbrev",
                    us.getCourseDescriptor().getDept().getAbbrev());
            uniqueel.setAttribute("course-id",
                    us.getCourse().getNumber().getUniqueString());

            for (SectionDescriptor sd : us.getSectionDescriptors()) {
                Element sectionel = new Element("section");
                uniqueel.addContent(sectionel);

                Section section = sd.getActualSection();
                SectionID id = section.getID();
                sectionel.setAttribute("id", id.getUniqueStringForm());
            }
        }

        Writer writer = new OutputStreamWriter(out, SAVED_SCHEDULE_CHARSET);
        XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());
        xmlout.output(doc, writer);
    }
}
