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

package edu.rpi.scheduler.schedb;

import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.DefensiveTools;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class SectionDescriptor implements Comparable<SectionDescriptor> {
    public static List<SectionDescriptor> getDescriptors(Department dept, Course course,
            Collection<Section> sections) {
        List<SectionDescriptor> sds = new ArrayList<SectionDescriptor>();
        for (Section section : sections) {
            sds.add(new SectionDescriptor(dept, course, section));
        }
        return sds;
    }

    public static List<SectionDescriptor> getDescriptors(CourseDescriptor course,
            Collection<Section> sections) {
        return getDescriptors(course.getDept(), course.getActualCourse(), sections);
    }

    private final Department dept;
    private final Course course;
    private final Section section;

    public SectionDescriptor(SectionDescriptor other) {
        this(other.getDept(), other.getCourse(), other.getActualSection());
    }

    public SectionDescriptor(CourseDescriptor other, Section section) {
        this(other.getDept(), other.getActualCourse(), section);
    }

    public SectionDescriptor(Department dept, Course course, Section section) {
        DefensiveTools.checkNull(dept, "dept");
        DefensiveTools.checkNull(course, "course");
        DefensiveTools.checkNull(section, "section");

        this.dept = dept;
        this.course = course;
        this.section = section;
    }

    public Department getDept() { return dept; }

    public Course getCourse() { return course; }

    public Section getActualSection() { return section; }

    public int compareTo(SectionDescriptor sd) {
        int deptc = getDept().compareTo(sd.getDept());
        if (deptc != 0) return deptc;

        int sc = getCourse().compareTo(sd.getCourse());
        if (sc != 0) return sc;

        return getActualSection().compareTo(sd.getActualSection());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SectionDescriptor)) return false;

        SectionDescriptor othersd = (SectionDescriptor) o;

        if (!getDept().equals(othersd.getDept())) return false;
        if (!getCourse().equals(othersd.getCourse())) return false;
        if (!getActualSection().equals(othersd.getActualSection())) return false;

        return true;
    }

    public int hashCode() {
        int result = getDept().hashCode();
        result = 29 * result + getCourse().hashCode();
        result = 29 * result + getActualSection().hashCode();
        return result;
    }

    public String toString() {
        return getDept().getName() + " " + getCourse().getNumber()
                + ": " + getCourse().getName() + " sec#" + getActualSection().getNumber();
    }
}
