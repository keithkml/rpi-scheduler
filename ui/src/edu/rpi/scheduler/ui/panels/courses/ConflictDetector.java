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

import edu.rpi.scheduler.CopyOnWriteArrayList;
import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.engine.SelectedCourse;
import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.BackgroundWorker;
import edu.rpi.scheduler.ui.SchedulingSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConflictDetector {
    private SchedulingSession session;
    private SelectedCoursesList selectedCoursesModel;

    private List<ConflictListener> listeners = new CopyOnWriteArrayList<ConflictListener>();

    private SchedulerEngine preScheduler;

    private Map<CourseDescriptor,Collection<SelectedCourse>> selectedConflicts
            = new HashMap<CourseDescriptor, Collection<SelectedCourse>>();
    private Map<CourseDescriptor,Boolean> cachedWouldFitAnySchedule
            = new HashMap<CourseDescriptor, Boolean>();

    private BackgroundWorker preschedulerWorker;
    private volatile boolean valid = false;

    public ConflictDetector(SchedulingSession session,
            SelectedCoursesList selectedCoursesModel) {
        this.session = session;
        this.selectedCoursesModel = selectedCoursesModel;

        selectedCoursesModel.addSelectedCoursesListener(new SelectedCoursesListener() {
            public void coursesAdded(SelectedCoursesList courseList,
                    Collection<SelectedCoursesList.SelectedCourseHolder> added) {
                for (SelectedCoursesList.SelectedCourseHolder holder : added) {
                    preScheduler.addCourse(new SelectedCourse(holder.getCourse(),
                            holder.isExtra()));
                }

                handleRequiredCoursesAdded(added);
            }

            public void coursesRemoved(SelectedCoursesList courseList,
                    Collection<SelectedCoursesList.SelectedCourseHolder> removed) {
                Set<CourseDescriptor> removedCourses = new HashSet<CourseDescriptor>();
                for (SelectedCoursesList.SelectedCourseHolder holder : removed) {
                    CourseDescriptor course = holder.getCourse();
                    preScheduler.removeCourse(course);
                    removedCourses.add(course);
                }

                handleRequiredCoursesRemoved(removedCourses);
            }

            public void coursesChanged(SelectedCoursesList courseList) {
                preScheduler.setSelectedCourses(courseList.getSelectedCourses());
                selectedConflicts.clear();
                cachedWouldFitAnySchedule.clear();
                setConflictsChanged();
            }

            public void courseStatusChanged(SelectedCoursesList courseList,
                    SelectedCoursesList.SelectedCourseHolder course,
                    boolean newExtra) {
                if (newExtra) {
                    preScheduler.removeCourse(course.getCourse());
                    handleRequiredCoursesRemoved(Collections.singleton(course.getCourse()));
                } else {
                    preScheduler.addCourse(new SelectedCourse(course.getCourse(),
                            course.isExtra()));
                    handleRequiredCoursesAdded(Collections.singleton(course));
                }
                setConflictsChanged();
            }
        });

        preScheduler = new SchedulerEngine(session.getEngine().getSchedulerData());
        preschedulerWorker = new BackgroundWorker();
        preschedulerWorker.start();
//        preScheduler.addEngineListener(new EngineListener() {
//            public void schedulesGenerated(SchedulerEngine engine) {
//                handleSchedulesGenerated();
//            }
//        });
    }

    private void handleSchedulesGenerated() {
        valid = preschedulerWorker.isQueueEmpty();
        fireConflictsChanged();
    }

    private void handleRequiredCoursesAdded(
            Collection<SelectedCoursesList.SelectedCourseHolder> added) {
        for (Iterator<Map.Entry<CourseDescriptor,Boolean>> it
                = cachedWouldFitAnySchedule.entrySet().iterator();
             it.hasNext();) {
            boolean wouldFit = it.next().getValue();
            if (wouldFit) {
                // if it would fit before, it might not now
                it.remove();
            }
        }
        selectedConflicts.clear();
        setConflictsChanged();
    }

    private void handleRequiredCoursesRemoved(Set<CourseDescriptor> removedCourses) {
        for (Iterator<Map.Entry<CourseDescriptor,Boolean>> it
                = cachedWouldFitAnySchedule.entrySet().iterator();
             it.hasNext();) {

            Map.Entry<CourseDescriptor,Boolean> entry = it.next();
            boolean wouldFit = entry.getValue();
            if (!wouldFit) {
                // if it wouldn't fit before, it might now
                it.remove();
            }
        }

        for (Collection<SelectedCourse> conflicts : selectedConflicts.values()) {
            for (Iterator<SelectedCourse> it = conflicts.iterator(); it.hasNext();) {
                SelectedCourse course = it.next();
                if (removedCourses.contains(course.getCourse())) it.remove();
            }
        }

        setConflictsChanged();
    }

    private void fireConflictsChanged() {
        assert !Thread.holdsLock(this);

        for (ConflictListener listener : listeners) {
            listener.conflictsUpdated(this);
        }
    }

    private void setConflictsChanged() {
        valid = false;
        fireConflictsChanged();
        preschedulerWorker.workOn(new Runnable() {
            public void run() {
                preScheduler.generateSchedules();
                handleSchedulesGenerated();
            }
        });
    }

    public boolean wouldFitAnySchedule(CourseDescriptor toAdd) {
        Boolean would = cachedWouldFitAnySchedule.get(toAdd);
        if (valid && would == null && !preScheduler.isWorking()) {
            List<Schedule> schedules = preScheduler.getGeneratedSchedules();
            if (schedules.isEmpty()) {
                would = true;
            } else {
                Outer: for (Schedule sched : schedules) {
                    for (Section section : toAdd.getActualCourse().getSections()) {
                        if (section.getWeekMask().fitsInto(sched.getTimeMask())) {
                            would = true;
                            break Outer;
                        }
                    }
                }
                if (would == null) would = false;
            }
            cachedWouldFitAnySchedule.put(toAdd, would);
        }

        if (would == null) return true;
        else return would;
    }

    public Collection<SelectedCourse> getSelectedConflicts(CourseDescriptor toAdd) {
        Collection<SelectedCourse> conflicts = selectedConflicts.get(toAdd);
        if (conflicts == null) {
            conflicts = new ArrayList<SelectedCourse>();
            for (SelectedCourse course : getSelectedCourses()) {
                CourseDescriptor otherCourse = course.getCourse();
                if (otherCourse.equals(toAdd)) continue;

                if (preScheduler.coursesConflict(toAdd, otherCourse)) {
                    conflicts.add(course);
                }
            }
            selectedConflicts.put(toAdd, conflicts);
        }
        return conflicts;
    }

    private Collection<SelectedCourse> getSelectedCourses() {
        return selectedCoursesModel.getSelectedCourses();
    }

    public boolean couldBeAdded(CourseDescriptor toAdd) {
        return wouldFitAnySchedule(toAdd);
    }

    public void addListener(ConflictListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConflictListener listener) {
        listeners.remove(listener);
    }
}
