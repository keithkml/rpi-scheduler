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

package edu.rpi.scheduler.ui.panels.courses.indexer;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Location;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.Professor;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.SectionID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CourseDocument implements IndexableDocument {
    public static final SearchKey KEY_DEPTNAME = new SearchKey("dept-name");
    public static final SearchKey KEY_DEPTABBREV = new SearchKey("dept-abbrev");
    public static final SearchKey KEY_COURSENAME = new SearchKey("course-name");
    public static final SearchKey KEY_COURSENUMBER = new SearchKey("course-number");
    public static final SearchKey KEY_SECTIONID = new SearchKey("section-id");
    public static final SearchKey KEY_NOTES = new SearchKey("notes");
    public static final SearchKey KEY_LOCATION = new SearchKey("location");
    public static final SearchKey KEY_PROFESSOR = new SearchKey("professor");

    private final CourseDescriptor cd;
    private Map<SearchKey,Set<String>> strings = null;

    public CourseDocument(CourseDescriptor course) {
        this.cd = course;
    }

    public CourseDescriptor getCourse() { return cd; }

    public Map<SearchKey, Set<String>> getStrings() {
        if (strings == null) strings = genStrings();
        return strings;
    }

    protected Map<SearchKey,Set<String>> genStrings() {
        Department dept = cd.getDept();
        Course course = cd.getActualCourse();

        Map<SearchKey, Set<String>> map = new HashMap<SearchKey, Set<String>>();
        map.put(KEY_DEPTNAME, Collections.singleton(dept.getName()));
        map.put(KEY_DEPTABBREV, Collections.singleton(dept.getAbbrev()));
        map.put(KEY_COURSENAME, Collections.singleton(course.getName()));
        map.put(KEY_COURSENUMBER,
                Collections.singleton(course.getNumber().toString()));

        Collection<Section> sections = course.getSections();
        Set<String> sectionIds = new HashSet<String>(sections.size());
        Set<String> noteList = null;
        Set<String> locationList = new HashSet<String>();
        Set<String> professorList = new HashSet<String>();
        for (Section section : sections) {
            SectionID id = section.getID();
            if (id != null) sectionIds.add(id.toString());
//            SectionNumber num = section.getNumber();
//            if (num != null) map.put(TYPE_, num.toString());
            Notes notes = section.getNotes();
            if (notes != null) {
                if (noteList == null) noteList = new HashSet<String>();
                noteList.addAll(notes.getNotesAsText());
            }
            Collection<ClassPeriod> periods = section.getPeriods();
            for (ClassPeriod period : periods) {
                Location location = period.getLocation();
                if (location != null) locationList.add(location.getName());
                Professor prof = period.getProfessor();
                if (prof != null) professorList.add(prof.getName());
            }
        }
        map.put(KEY_SECTIONID, sectionIds);
        map.put(KEY_NOTES, noteList);
        map.put(KEY_LOCATION, locationList);
        map.put(KEY_PROFESSOR, professorList);
        return map;
    }
}
