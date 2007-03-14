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

import java.util.LinkedList;
import java.util.List;

public class Course implements JdomExternalizable {
    private final int number;
    private final String name;
    private final IntRange credits;
    private final GradeType gradeType;
    private final List<Section> sections = new LinkedList<Section>();
    private final List<String> notes = new LinkedList<String>();

    public Course(int id, String name, IntRange credits, GradeType type,
            int seats) {
        if (credits == null) throw new NullPointerException("credits");
        
        this.number = id;
        this.name = name;
        this.credits = credits;
        this.gradeType = type;
    }

    public int getNumber() {
        return number;
    }

    public void addSection(Section section) {
        sections.add(section);
    }

    public void addNote(String note) {
        if (!notes.contains(note)) notes.add(note);
    }

    public boolean writeExternal(Element parent) {
        Element coursel = new Element("course");
        coursel.setAttribute("number", Integer.toString(number));
        coursel.setAttribute("name", name);

        coursel.setAttribute("min-credits", Integer.toString(credits.getFrom()));
        int to = credits.isRange() ? credits.getTo() : credits.getFrom();
        coursel.setAttribute("max-credits", Integer.toString(to));

        String gtstr = null;
        if (gradeType == GradeType.NORMAL) gtstr = "normal";
        else if (gradeType == GradeType.PASSFAIL) gtstr = "pass-fail";
        else if (gradeType == GradeType.NOGRADE) gtstr = "no-grade";
        if (gtstr != null) coursel.setAttribute("grade-type", gtstr);

        boolean anySections = false;
        for (Section section : sections) {
            if (section.writeExternal(coursel)) anySections = true;
        }

        for (String s : notes) {
            Element notel = new Element("note");
            notel.addContent(s);
            coursel.addContent(notel);
        }

        if (anySections) parent.addContent(coursel);
        else System.err.println("Skipping " + name);

        return anySections;
    }

    public String toString() {
        return "#" + number + "-" + name + " (" + credits + " credits):"
                + sections + (notes.isEmpty() ? "" : "notes: " + notes);
    }
}
