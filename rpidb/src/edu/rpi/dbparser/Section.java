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

public class Section implements JdomExternalizable {
    private final int crn;
    private final int number;
    private final String location;
    private final int seats;
    private final List<Period> periods = new LinkedList<Period>();

    public Section(int crn, int number, String location, int seats) {
        this.crn = crn;
        this.number = number;
        this.location = location;
        this.seats = seats;
    }

    public int getCrn() {
        return crn;
    }

    public int getNumber() { return number; }

    public String getLocation() {
        return location;
    }

    public int getSeats() {
        return seats;
    }

    public void addPeriod(Period period) {
        periods.add(period);
    }

    public boolean writeExternal(Element parent) {
        Element secel = new Element("section");
        secel.setAttribute("crn", Integer.toString(crn));
        secel.setAttribute("number", Integer.toString(number));
        secel.setAttribute("seats", Integer.toString(seats));
        if (location != null) secel.setAttribute("location", location);

        boolean anyPeriods = false;
        for (Period period : periods) {
            if (period.writeExternal(secel)) anyPeriods = true;
        }

        if (anyPeriods) parent.addContent(secel);
        return anyPeriods;
    }

    public String toString() {
        return "#" + number + (location != null ? " (in " + location + ", "
                + seats + " seats)" : "") + " - " + periods;
    }
}
