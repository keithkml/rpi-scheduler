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
import edu.rpi.scheduler.schedb.spec.SectionNumber;
import edu.rpi.scheduler.DefensiveTools;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a set of sections of a course with the same time mask. This is
 * common with popular courses.
 */
public final class UniqueSection {
    public static UniqueSection getInstance(SectionDescriptor section) {
        DefensiveTools.checkNull(section, "section");

        WeekMask<?> mask = section.getActualSection().getWeekMask();
        UniqueSection us = new UniqueSection(mask);
        us.addSection(section);
        return us;
    }

    private CourseDescriptor course = null;
    private final Set<SectionDescriptor> sections = new HashSet<SectionDescriptor>();
    private final WeekMask<?> mask;
    private SectionNumber lowest = null;

    /**
     * Creates a new {@code UniqueSection} with the given time mask.
     * @param mask the time mask of this set of sections
     */
    public UniqueSection(WeekMask<?> mask) {
        this.mask = mask;
    }

    /**
     * Returns the time mask of these sections.
     * @return the time mask of the sections represented
     */
    public WeekMask<?> getTimeMask() { return mask; }

    /**
     * Adds a section whose time mask matches this {@code UniqueSection}'s.
     * @param section the section to add to this {@code UniqueSection}
     */
    public void addSection(SectionDescriptor section) {
        CourseDescriptor cd = CourseDescriptor.getInstanceForSection(section);
        CourseDescriptor course = this.course;
        if (course == null) {
            course = cd;
            this.course = course;
            
        } else if (!cd.equals(course)) {
            throw new IllegalArgumentException("section " + section
                    + " cannot be added because it is not in course " + course);
        }

        SectionNumber number = section.getActualSection().getNumber();
        if (lowest == null || number.compareTo(lowest) == -1) {
            lowest = number;
        }
        sections.add(section);
    }

    /**
     * Returns the lowest section number represented, for use in sorting.
     * @return the lowest section number represented by this
     *         {@code UniqueSection}
     */
    public SectionNumber getLowestNumber() { return lowest; }

    public Course getCourse() {
        return course.getActualCourse();
    }

    public CourseDescriptor getCourseDescriptor() {
        return course;
    }

    /**
     * Returns the represented sections.
     * @return the sections represented by this {@code UniqueSection}
     */
    public Collection<SectionDescriptor> getSectionDescriptors() {
        return sections;
    }
}
