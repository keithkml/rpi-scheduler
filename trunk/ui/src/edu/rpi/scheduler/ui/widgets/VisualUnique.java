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

package edu.rpi.scheduler.ui.widgets;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Section;

import java.awt.Paint;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class VisualUnique {
    private static final Comparator<VisualSection> VSCOMPARATOR
            = new Comparator<VisualSection>() {
        public int compare(VisualSection o1, VisualSection o2) {
            return o1.getSection().getNumber().compareTo(
                    o2.getSection().getNumber());
        }
    };

    private final CourseDescriptor course;
    private final UniqueSection section;
    private final List<VisualSection> sections;
    private Paint paint = null;
    private String description = null;
    private int current = 0;
    private boolean selected = false;

    public VisualUnique(UniqueSection section, List<VisualSection> sections) {
        this(section, sections, null);
    }

    public VisualUnique(UniqueSection section, List<VisualSection> sections,
            Paint paint) {
        this.course = CourseDescriptor.getInstanceForSection(sections.get(0).getSectionDesc());
        this.section = section;
        this.sections = sections;
        this.paint = paint;

        Collections.sort(sections, VSCOMPARATOR);

        for (VisualSection visualSection : sections) {
            visualSection.setVisualUnique(this);
        }
    }

    private String generateDesc() {
        StringBuffer buffer = new StringBuffer();

        CourseDescriptor course = this.course;
        buffer.append(course.getDept().getAbbrev());
        buffer.append(' ');
        buffer.append(course.getActualCourse().getNumber());
        buffer.append(' ');
        boolean first = true;
        List<VisualSection> sections = this.sections;
        for (VisualSection vs : sections) {
            Section section = vs.getSection();

            if (!first) buffer.append('/');
            else first = false;

            buffer.append(section.getNumber());
        }
        buffer.append(": ");
        buffer.append(course.getActualCourse().getName());
        buffer.append(" (");

        first = true;
        for (VisualSection vs : sections) {
            Section section = vs.getSection();

            if (!first) buffer.append("/");
            else first = false;

            buffer.append(section.getID().toString());
        }

        buffer.append(')');

        return buffer.toString();
    }

    public UniqueSection getUniqueSection() {
        return section;
    }

    public Paint getPaint() {
        return paint;
    }
    
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public VisualSection getCurrentSection() {
        return sections.get(current);
    }

    public int getCurrentIndex() {
        return current;
    }

    public void setCurrentIndex(int current) {
        this.current = current;
    }

    public Course getCourse() {
        return course.getActualCourse();
    }

    public CourseDescriptor getCourseDesc() { return course; }

    public List<VisualSection> getSections() {
        return sections;
    }

    public String getDescription() {
        if (description == null) description = generateDesc();
        return description;
    }

    public void setSelected(boolean selected) { this.selected = selected; }

    public boolean isSelected() { return selected; }

}
