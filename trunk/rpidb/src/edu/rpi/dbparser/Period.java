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

public class Period implements JdomExternalizable {
    private static final String[] DAYNAMES = new String[] {
        "mon", "tue", "wed", "thu", "fri", "sat", "sun"
    };

    private final PeriodType type;
    private final String professor;
    private final boolean[] days;
    private final Time start;
    private final Time end;

    public Period(PeriodType type, boolean[] days, Time start,
            Time end, String professor) {
        if (type == null) throw new NullPointerException("type");
        if (days.length != 7) {
            throw new IllegalArgumentException("days must contain 7 elements "
                    + "(it contains " + days.length + ")");
        }
        this.type = type;
        this.days = days;
        this.start = start;
        this.end = end;
        this.professor = professor;
    }

    public boolean writeExternal(Element parent) {
        Element element = new Element("period");
        String typestr = null;
        if (type == PeriodType.INDSTUDY) typestr = "ind-study";
        else if (type == PeriodType.LAB) typestr = "lab";
        else if (type == PeriodType.LECTURE) typestr = "lecture";
        else if (type == PeriodType.RECITATION) typestr = "recitation";
        else if (type == PeriodType.SEMINAR) typestr = "seminar";
        else if (type == PeriodType.STUDIO) typestr = "studio";
        else if (type == PeriodType.UNKNOWN) typestr = "unknown";

        if (typestr != null) element.setAttribute("type", typestr);

        if (professor != null) element.setAttribute("professor", professor);

        StringBuffer sb = new StringBuffer(30);
        boolean first = true;
        for (int i = 0; i < days.length; i++) {
            if (days[i]) {
                if (!first) sb.append(",");
                else first = false;

                sb.append(DAYNAMES[i]);
            }
        }

        element.setAttribute("days", sb.toString());

        element.setAttribute("starts", start.toString());
        element.setAttribute("ends", end.toString());

        parent.addContent(element);
        
        return true;
    }

    public String toString() {
        return type + " <" + start + "-" + end + "> (" + professor + ")";
    }
}
