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

import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.DefensiveTools;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class CourseDescriptor implements Comparable<CourseDescriptor> {
    public static List<CourseDescriptor> getDescriptors(Department dept,
            Collection<Course> courses) {
        List<CourseDescriptor> cds = new ArrayList<CourseDescriptor>();
        for (Course course : courses) {
            cds.add(new CourseDescriptor(dept, course));
        }
        return cds;
    }

    private final Department dept;
    private final Course course;

    private CourseDescriptor(SectionDescriptor other) {
        this(other.getDept(), other.getCourse());
    }

    public CourseDescriptor(CourseDescriptor other) {
        this(other.getDept(), other.getActualCourse());
    }

    public CourseDescriptor(Department dept, Course course) {
        DefensiveTools.checkNull(dept, "dept");
        DefensiveTools.checkNull(course, "course");

        this.dept = dept;
        this.course = course;
    }

    public Department getDept() { return dept; }

    public Course getActualCourse() { return course; }

    public int compareTo(CourseDescriptor sd) {
        int deptc = getDept().compareTo(sd.getDept());
        if (deptc != 0) return deptc;

        return getActualCourse().compareTo(sd.getActualCourse());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseDescriptor)) return false;

        CourseDescriptor othersd = (CourseDescriptor) o;

        if (!getDept().equals(othersd.getDept())) return false;
        if (!getActualCourse().equals(othersd.getActualCourse())) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getDept().hashCode();
        result = 29 * result + getActualCourse().hashCode();
        return result;
    }

    public String toString() {
        return getDept().getAbbrev() + " " + getActualCourse().getNumber()
                + ": " + getActualCourse().getName();
    }

    public static CourseDescriptor getInstanceForSection(SectionDescriptor other) {
        return new CourseDescriptor(other);
    }
}
