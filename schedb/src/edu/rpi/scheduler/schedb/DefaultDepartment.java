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

import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.CourseID;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultDepartment extends AbstractDepartment {
    private final Map<CourseID,Course> courseMap = new HashMap<CourseID, Course>(50);

    public DefaultDepartment(String abbrev) {
        this(abbrev, abbrev);
    }

    public DefaultDepartment(String abbrev, String name) {
        super(abbrev, name);
    }

    public void addCourse(Course course) {
        courseMap.put(course.getNumber(), course);
    }

    public Course getCourse(CourseID courseid) {
        return courseMap.get(courseid);
    }

    public Collection<Course> getCourses() {
        return courseMap.values();
    }

    public boolean containsCourse(Course course) {
        return courseMap.values().contains(course);
    }

    public Course getCourseWithCourseIdString(String courseIdString) {
        for (Course course : getCourses()) {
            if (course.getNumber().getUniqueString().equals(courseIdString)) {
                return course;
            }
        }
        return null;
    }
}
