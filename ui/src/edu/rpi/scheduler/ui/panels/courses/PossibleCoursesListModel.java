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
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.panels.courses.indexer.TinyIndexer;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchKey;
import edu.rpi.scheduler.ui.panels.courses.indexer.IndexableDocument;
import edu.rpi.scheduler.ui.panels.courses.indexer.CourseDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides a list model that contains the courses in a specific department.
 */
public class PossibleCoursesListModel extends CoursesListModel {
    private List<CourseDescriptor> courses = new ArrayList<CourseDescriptor>();
    private Set<CourseDescriptor> used = new HashSet<CourseDescriptor>();
    private Department dept = null;
    private String filter = null;
    private String optFilter = "";
    private final SchedulingSession session;

    private Map<CourseDescriptor,CourseDocument> matches
            = new HashMap<CourseDescriptor, CourseDocument>(2000);
    private TinyIndexer indexer = new TinyIndexer();
    private Set<SearchKey> filterKeys = Collections.emptySet();


    /**
     * Creates a courses-in-department list model with no associated department,
     * and so no items.
     */
    public PossibleCoursesListModel(SchedulingSession session) {
        this(session, null);
    }

    /**
     * Creates a model containing the courses in the given department.
     * @param dept the department to display
     * @see #setDept
     */
    public PossibleCoursesListModel(SchedulingSession session, Department dept) {
        this.session = session;
        this.dept = dept;

        for (CourseDescriptor course : session.getEngine().getAllCourses()) {
            indexer.registerObject(new CourseDocument(course));
        }

        setFilter("");
    }

    public Set<SearchKey> getFilterKeys() {
        return filterKeys;
    }

    public void setFilterKeys(Set<SearchKey> filterKeys) {
        this.filterKeys = filterKeys;
        updateMatches();
        updateDisplayedCourses();
    }

    public String getFilter() { return filter; }

    public void setFilter(String filter) {
        String optFilter = filter;
        if (optFilter == null) optFilter = "";
        optFilter = optFilter.toLowerCase().trim();
        this.filter = filter;
        this.optFilter = optFilter;

        updateMatches();
        updateDisplayedCourses();
    }

    private void updateMatches() {
        matches.clear();
        // if the filterKeys is empty, we mean "all fields"
        Set<SearchKey> keys = filterKeys;
        if (keys != null && keys.isEmpty()) keys = null;
        Set<IndexableDocument> matches = indexer.getMatches(optFilter, keys);
        for (IndexableDocument document : matches) {
            CourseDocument cd = (CourseDocument) document;
            this.matches.put(cd.getCourse(), cd);
        }
    }

    private void updateDisplayedCourses() {
        List<CourseHolder> courses = getDisplayedCourses();
        int oldSize = courses.size();
        List<CourseHolder> newcourses = new ArrayList<CourseHolder>(matches.keySet().size());
        for (CourseDescriptor course : matches.keySet()) {
            if (dept == null || dept.equals(course.getDept())) {
                newcourses.add(new CourseHolder(course));
            }
        }
        if (courses.equals(newcourses)) return;

        courses.clear();
        courses.addAll(newcourses);
        Collections.sort(courses);
        refillCourses(oldSize);
    }

    private void refillCourses(int oldSize) {
        List<CourseHolder> courses = getDisplayedCourses();
        if (oldSize != 0) {
            setDisplayedCourses(Collections.<CourseHolder>emptyList());
            fireIntervalRemoved(this, 0, oldSize - 1);
        }
        setDisplayedCourses(courses);
        if (!courses.isEmpty()) fireIntervalAdded(this, 0, courses.size() - 1);
    }

    /**
     * Sets the list model to display the courses in the given department.
     * @param dept the department to display
     */
    public void setDept(Department dept) {
        this.dept = dept;
        updateDisplayedCourses();
    }

    /**
     * Sets the list of "used courses," essentially courses that will not be
     * displayed even if they are in this department. Note that this list can
     * contain any number of {@code Course}s, whether or not they are in
     * the {@linkplain #setDept current department}.
     * @param used the list of courses to hide from the user
     */
    public void setUsedCourses(List<CourseDescriptor> used) {
        this.used = new HashSet<CourseDescriptor>(used);

        updateUsed();
    }

    private int findPos(List<CourseDescriptor> courses,
            List<? extends CourseHolder> displayed, int end) {
        int realend = Math.min(displayed.size(), end);
        for (ListIterator<? extends CourseHolder> it = displayed.listIterator(realend);
             it.hasPrevious();) {
            CourseDescriptor c = it.previous().getCourseDescriptor();

            for (int i = end - 1; i >= 0; i--) {
                if (courses.get(i).equals(c)) {
                    return it.previousIndex() + 2;
                }
            }
        }

        return 0;
    }

    private void updateUsed() {
        Department dept = this.dept;
        List<CourseDescriptor> courses = this.courses;
        int oldsize = courses.size();

        List<CourseHolder> displayedCourses = getDisplayedCourses();
        {
            for (Iterator<CourseHolder> it = displayedCourses.iterator(); it.hasNext();) {
                CourseHolder holder = it.next();

                CourseDescriptor course = holder.getCourseDescriptor();
                if ((dept == null || !course.getDept().equals(dept)) && filterMatches(course)) {
                    it.remove();
//                    fireIntervalRemoved(this, i, i);
                }
            }
        }

        int i = 0;
        for (CourseDescriptor course : courses) {
            boolean notUsed = !used.contains(course);
            if (notUsed && filterMatches(course)) {
                if (indexOf(course) == -1) {
                    int pos = findPos(courses, displayedCourses, i);
                    displayedCourses.add(pos, new CourseHolder(course));
//                    fireIntervalAdded(this, pos, pos);
                }

            } else {
                int index = indexOf(course);
                if (index != -1) {
                    displayedCourses.remove(index);
//                    fireIntervalRemoved(this, index, index);
                }
            }
            i++;
        }
        refillCourses(oldsize);
    }

    private boolean filterMatches(CourseDescriptor cd) {
        return matches.containsKey(cd);
    }

    public List<CourseDescriptor> getCourses(int[] indices) {
        List<CourseDescriptor> selected = new ArrayList<CourseDescriptor>();
        for (int i : indices) selected.add(getCourseAt(i));
        return selected;
    }

    public void notifyChanged() {
        fireContentsChanged(this, 0, courses.size());
    }
}
