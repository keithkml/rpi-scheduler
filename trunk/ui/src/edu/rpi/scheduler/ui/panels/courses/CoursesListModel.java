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

package edu.rpi.scheduler.ui.panels.courses;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.spec.Course;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a {@code ListModel} containing a list of {@code Course}s.
 */
public abstract class CoursesListModel extends AbstractListModel {
    private List<CourseHolder> displayedCourses = new ArrayList<CourseHolder>();

    /**
     * Returns the courses stored in this list model.
     * @return the courses contained in this list model
     */
    public List<CourseDescriptor> getCourses() {
        List<CourseDescriptor> courseArray = new ArrayList<CourseDescriptor>();

        for (CourseHolder courseHolder : displayedCourses) {
            courseArray.add(courseHolder.getCourseDescriptor());
        }

        return courseArray;
    }

    public int getSize() {
        return displayedCourses.size();
    }

    /**
     * Gets the {@code Course} at the given index in the list. A thin
     * wrapper around {@link ListModel#getElementAt}.
     * @param index an index into the list
     * @return the {@code Course} at the given list index
     */
    public CourseDescriptor getCourseAt(int index) {
        CourseHolder courseHolder = (CourseHolder) getElementAt(index);
        return courseHolder == null ? null : courseHolder.getCourseDescriptor();
    }

    public Object getElementAt(int index) {
        return (index >= displayedCourses.size() || index < 0)
                ? null : displayedCourses.get(index);
    }

    protected final int indexOf(CourseDescriptor course) {
        int i = 0;
        for (CourseHolder cur : displayedCourses) {
            if (cur.getCourseDescriptor().equals(course)) return i;
            i++;
        }

        return -1;
    }

    public List<CourseHolder> getDisplayedCourses() { return displayedCourses; }

    public void setDisplayedCourses(List<CourseHolder> displayedCourses) {
        this.displayedCourses = displayedCourses;
    }

    protected static class CourseHolder implements Comparable<CourseHolder> {
        private final CourseDescriptor course;

        public CourseHolder(CourseDescriptor course) {
            this.course = course;
        }

        public Course getCourse() { return course.getActualCourse(); }
        public CourseDescriptor getCourseDescriptor() { return course; }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CourseHolder)) return false;

            final CourseHolder courseHolder = (CourseHolder) o;

            if (!course.equals(courseHolder.course)) return false;

            return true;
        }

        public int hashCode() {
            return course.hashCode();
        }

        public String toString() {
            Course ac = course.getActualCourse();
            return ac.getNumber() + " - " + ac.getName();
        }

        public int compareTo(CourseHolder o) {
            return getCourseDescriptor().compareTo(o.getCourseDescriptor());
        }
    }
}
