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

package edu.rpi.dbparser;

import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class Department implements JdomExternalizable {
    private final String abbrev;
    private final String name;
    private final List<Course> courses = new ArrayList<Course>();

    public Department(String abbrev, String name) {
        if (abbrev == null && name == null) {
            throw new IllegalArgumentException("department without name or "
                    + "abbreviation");
        }

        this.abbrev = abbrev;
        this.name = name;
    }

    public String getAbbrev() { return abbrev; }

    public void addCourse(Course course) {
        courses.add(course);
    }

    public boolean writeExternal(Element parent) {
        Element deptel = new Element("dept");
        if (abbrev != null) deptel.setAttribute("abbrev", abbrev);
        if (name != null) deptel.setAttribute("name", name);
        boolean anyCourses = false;
        for (Course course : courses) {
            if (course.writeExternal(deptel)) anyCourses = true;
        }
        if (anyCourses) parent.addContent(deptel);
        return anyCourses;
    }

    public String toString() {
        return abbrev + ": " + courses;
    }
}
