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
import edu.rpi.scheduler.engine.SelectedCourse;
import edu.rpi.scheduler.ui.SchedulingSession;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;

public class SelectedCoursesList extends AbstractTableModel {
    private static final List<String> COLUMN_NAMES = Arrays.asList("Extra", "Course");
    private static final List<Class<?>> COLUMN_CLASSES
            = Arrays.<Class<?>>asList(Boolean.class, String.class);

    private List<SelectedCourseHolder> courses = new ArrayList<SelectedCourseHolder>();
    private List<SelectedCoursesListener> listeners = new ArrayList<SelectedCoursesListener>();

    private final SchedulingSession session;
    public static final int COL_EXTRA = 0;
    public static final int COL_COURSE = 1;

    public SelectedCoursesList(SchedulingSession session) {
        this.session = session;
    }

    public void addSelectedCoursesListener(SelectedCoursesListener l) {
        listeners.add(l);
    }

    public void removeSelectedCoursesListener(SelectedCoursesListener l) {
        listeners.remove(l);
    }

    public boolean addCourse(CourseDescriptor cd, boolean extra) {
        for (SelectedCourseHolder holder : courses) {
            if (holder.getCourse().equals(cd)) return false;
        }

        SelectedCourseHolder holder = new SelectedCourseHolder(cd, extra);
        courses.add(holder);

        int ind = courses.size() - 1;
        fireTableRowsInserted(ind, ind);
        fireCourseAdded(holder);
        return true;
    }

    public void removeCourses(int[] rows) {
        List<SelectedCourse> courses = getCourses(rows);
        Collection<CourseDescriptor> cds = new ArrayList<CourseDescriptor>(courses.size());
        for (SelectedCourse course : courses) {
            cds.add(course.getCourse());
        }
        removeCourses(cds);
    }
    private void oldremoveCourses(int[] rows) {
        List<SelectedCourseHolder> removed = new ArrayList<SelectedCourseHolder>(rows.length);
        int off = 0;
        for (int i : rows) {
            int index = i - off;
            removed.add(courses.remove(index));
            fireTableRowsDeleted(index, index);
            off++;
        }
        fireCoursesRemoved(removed);
    }

    public void removeCourses(Collection<CourseDescriptor> toRemove) {
        Collection<SelectedCourseHolder> removed = new ArrayList<SelectedCourseHolder>();
        int i = 0;
        for (Iterator<SelectedCourseHolder> it = courses.iterator(); it.hasNext();) {
            SelectedCourseHolder holder = it.next();
            if (toRemove.contains(holder.getCourse())) {
                removed.add(holder);
                it.remove();
                fireTableRowsDeleted(i, i);
                i--;
            }
            i++;
        }
        fireCoursesRemoved(removed);
    }


    public void removeCourse(CourseDescriptor cd) {
        int i = 0;
        for (Iterator<SelectedCourseHolder> it = courses.iterator();
             it.hasNext();) {
            SelectedCourseHolder holder = it.next();
            if (holder.getCourse().equals(cd)) {
                it.remove();
                fireTableRowsDeleted(i, i);
                fireCourseRemoved(holder);
                break;
            }
            i++;
        }
    }

    public void setCourses(Collection<SelectedCourse> list) {
        int oldsize = courses.size();
        courses.clear();
        if (oldsize > 0) fireTableRowsDeleted(0, oldsize - 1);
        for (SelectedCourse course : list) {
            courses.add(new SelectedCourseHolder(course.getCourse(), course.isExtra()));
        }
        if (courses.size() > 0) fireTableRowsInserted(0, courses.size() - 1);
        fireCoursesChanged();
    }

    private void fireCourseAdded(SelectedCourseHolder holder) {
        fireCoursesAdded(Collections.singletonList(holder));
    }

    private void fireCoursesAdded(Collection<SelectedCourseHolder> list) {
        for (SelectedCoursesListener listener : listeners) {
            listener.coursesAdded(this, list);
        }
    }

    private void fireCourseRemoved(SelectedCourseHolder holder) {
        fireCoursesRemoved(Collections.singletonList(holder));
    }

    private void fireCoursesRemoved(Collection<SelectedCourseHolder> removed) {
        for (SelectedCoursesListener listener : listeners) {
            listener.coursesRemoved(this, removed);
        }
    }

    private void fireCoursesChanged() {
        for (SelectedCoursesListener listener : listeners) {
            listener.coursesChanged(this);
        }
    }

    private void fireExtraChanged(SelectedCourseHolder course,
            boolean newExtra) {
        for (SelectedCoursesListener listener : listeners) {
            listener.courseStatusChanged(this, course, newExtra);
        }
    }

    public Collection<SelectedCourse> getSelectedCourses() {
        Collection<SelectedCourse> list
                = new ArrayList<SelectedCourse>(courses.size());
        for (SelectedCourseHolder courseHolder : courses) {
            list.add(new SelectedCourse(courseHolder.getCourse(),
                    courseHolder.isExtra()));
        }
        return list;
    }

    public List<SelectedCourse> getCourses(int[] rows) {
        List<SelectedCourse> list = new ArrayList<SelectedCourse>(rows.length);
        for (int i : rows) {
            SelectedCourseHolder holder = courses.get(i);
            list.add(new SelectedCourse(holder.getCourse(), holder.isExtra()));
        }
        return list;
    }

    public CourseDescriptor getCourse(int row) {
        return courses.get(row).getCourse();
    }

    public boolean containsCourse(CourseDescriptor cd) {
        for (SelectedCourseHolder holder : courses) {
            if (holder.getCourse().equals(cd)) return true;
        }
        return false;
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES.get(column);
    }

    public int findColumn(String columnName) {
        return COLUMN_NAMES.indexOf(columnName);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES.get(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COL_EXTRA;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != COL_EXTRA) {
            throw new IllegalArgumentException("can't modify column "
                    + columnIndex);
        }
        boolean newExtra = (Boolean) aValue;
        SelectedCourseHolder course = courses.get(rowIndex);
        if (course.isExtra() == newExtra) return;

        course.setExtra(newExtra);
        fireTableCellUpdated(rowIndex, columnIndex);
        fireExtraChanged(course, newExtra);
    }

    public int getRowCount() { return courses.size(); }

    public int getColumnCount() { return 2; }

    public Object getValueAt(int rowIndex, int columnIndex) {
        SelectedCourseHolder cd = courses.get(rowIndex);
        if (columnIndex == COL_COURSE) {
            return cd.getCourse().getActualCourse().getName();
        } else if (columnIndex == COL_EXTRA) {
            return cd.isExtra();
        } else {
            throw new IllegalArgumentException("no such column " + columnIndex);
        }
    }

    public int getIndexOf(CourseDescriptor course) {
        int index = 0;
        for (SelectedCourseHolder holder : courses) {
            if (holder.getCourse().equals(course)) return index;
            index++;
        }
        return -1;
    }

    public static class SelectedCourseHolder {
        private final CourseDescriptor course;
        private boolean extra;

        public SelectedCourseHolder(CourseDescriptor course, boolean extra) {
            this.course = course;
            this.extra = extra;
        }

        public CourseDescriptor getCourse() { return course; }

        public boolean isExtra() { return extra; }

        private void setExtra(boolean extra) { this.extra = extra; }

        public String toString() {
            return "SelectedCourseHolder: " +
                    "course=" + course +
                    ", extra=" + extra;
        }
    }
}
