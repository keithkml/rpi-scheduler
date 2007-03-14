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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Time {
    private static final Pattern timeRE = Pattern.compile("(\\d+):(\\d+)");

    public static Time[] getRange(String start, String end, String endpm) {
        Matcher sm = timeRE.matcher(start);
        Matcher em = timeRE.matcher(end);
        if (!sm.matches() || !em.matches()) return null;

        int shr = Integer.parseInt(sm.group(1).trim());
        int smin = Integer.parseInt(sm.group(2).trim());
        int ehr = Integer.parseInt(em.group(1).trim());
        int emin = Integer.parseInt(em.group(2).trim());

        boolean spm; // undefined
        boolean epm = endpm.equalsIgnoreCase("PM");

        // the starting time is PM if the ending time is PM and the starting
        // hour is greater than the ending hour (this will break if RPI ever
        // offers 13-hour classes)
        int fakeehr;
        if (ehr == 12) fakeehr = 0;
        else fakeehr = ehr;

        int fakeshr;
        if (shr == 12) fakeshr = 0;
        else fakeshr = shr;

        spm = (epm && fakeshr <= fakeehr);

        return new Time[] { new Time(shr, smin, spm),
                            new Time(ehr, emin, epm) };
    }

    private final int hours;
    private final int minutes;
    private final boolean pm;

    public Time(int hours, int minutes, boolean pm) {

        this.hours = hours;
        this.minutes = minutes;
        this.pm = pm;
    }

    public String toString() {
        String minstr = Integer.toString(minutes);
        while (minstr.length() < 2) minstr = '0' + minstr;

        return hours + ":" + minstr + (pm ? "PM" : "AM");
    }
}
