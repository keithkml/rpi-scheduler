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

package edu.rpi.scheduler.schedb.load;

import edu.rpi.scheduler.schedb.DefaultCourse;
import edu.rpi.scheduler.schedb.DefaultGradeTypes;
import edu.rpi.scheduler.schedb.IntRange;
import edu.rpi.scheduler.schedb.StringCourseID;
import edu.rpi.scheduler.schedb.load.spec.CourseLoader;
import edu.rpi.scheduler.schedb.load.spec.DataElement;
import edu.rpi.scheduler.schedb.load.spec.NotesLoader;
import edu.rpi.scheduler.schedb.load.spec.SectionLoader;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.GradeType;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.Section;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.COURSE;

public class XmlCourseLoader extends XmlLoader implements CourseLoader {
    private static final Logger logger
            = Logger.getLogger(XmlDeptLoader.class.getPackage().getName());

    public XmlCourseLoader(MutableDataLoadingContext context) {
        super(context);
    }

    /*
    number="1010" name="ORAL COMM FOR TA'S I" seats="15" min-credits="0"
    max-credits="0" grade-type="pass-fail"
    */
    public Course loadCourse(DataElement element)
            throws DbLoadException {
        XmlDataElement xel = (XmlDataElement) element;

        Element coursel = xel.getDomElement();

        String number = readNumber(coursel);
        String name = readName(coursel);

        String use = null;
        if (name != null) use = name;
        else if (number != null) use = number;
        if (use != null) {
            getContext().fireLoadingObject(COURSE, use);
        }

        int minCred = readMinCredits(coursel);
        int maxCred = readMaxCredits(coursel);
        GradeType gradeType = readGradeType(coursel);

        Notes notesobj = readNotes(coursel);

        DefaultCourse course = createCourse(name, number,
                gradeType, minCred, maxCred, notesobj);

        readSections(coursel, course);

        readCustomData(coursel, course);

        return course;
    }

    protected void readCustomData(Element coursel, Course course) {
    }

    protected DefaultCourse createCourse(String name, String number,
            GradeType gradeType, int minCred, int maxCred,
            Notes notesobj) {
        return new DefaultCourse(name, new StringCourseID(number), gradeType,
                new IntRange(minCred, maxCred), notesobj);
    }

    protected void readSections(Element coursel, DefaultCourse course)
            throws DbLoadException {
        NodeList secels = coursel.getElementsByTagName("section");

        for (int i = 0; i < secels.getLength(); i++) {
            Element secel = (Element) secels.item(i);
            Section section;
            try {
                section = parseSection(secel);
            } catch (Exception e) {
                logger.log(Level.WARNING, "DB format exception while loading "
                        + "course '" + course.getName() + "'", e);
                continue;
            }
            if (section != null) course.addSection(section);
        }
    }

    protected GradeType readGradeType(Element coursel) throws DbLoadException {
        if (!coursel.hasAttribute("grade-type")) {
            return DefaultGradeTypes.NORMAL;
        }
        String typestr = coursel.getAttribute("grade-type");
        if (typestr.equals("pass-fail")) return DefaultGradeTypes.PASSFAIL;
        else return null;
    }

    protected int readMaxCredits(Element coursel) throws DbLoadException {
        return getInt(coursel, "max-credits");
    }

    protected int readMinCredits(Element coursel) throws DbLoadException {
        return getInt(coursel, "min-credits");
    }

    protected String readName(Element coursel) throws DbLoadException {
        return getString(coursel, "name");
    }

    protected String readNumber(Element coursel) throws DbLoadException {
        return getString(coursel, "number");
    }

    protected String getString(Element coursel, String attrname)
            throws DbLoadException {
        if (!coursel.hasAttribute(attrname)) return null;
        else return coursel.getAttribute(attrname);
    }

    protected int getInt(Element coursel, String attrname)
            throws DbLoadException {
        if (!coursel.hasAttribute(attrname)) return -1;
        else {
            try {
                return Integer.parseInt(coursel.getAttribute(attrname));
            } catch (NumberFormatException e) {
                throw new DbLoadException(coursel.getAttribute(attrname), e);
            }
        }
    }

    protected Notes readNotes(Element coursel) throws DbLoadException {
        NotesLoader loader = getContext().getNotesLoader();
        return loader.readNotes(new XmlDataElement(coursel));
    }

    protected Section parseSection(Element secel)
            throws DbLoadException {
        SectionLoader secLoader = getContext().getSectionLoader();
        return secLoader.loadSection(new XmlDataElement(secel));
    }
}
