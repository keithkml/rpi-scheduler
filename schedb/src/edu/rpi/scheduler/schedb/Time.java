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

import edu.rpi.scheduler.DefensiveTools;

public final class Time implements Comparable<Time> {
    public static final Ampm AM = Ampm.AM;
    public static final Ampm PM = Ampm.PM;

    private final int hours;
    private final int minutes;
    private final boolean pm;

    public Time(int hours, int minutes) {
        DefensiveTools.checkRange(hours, "hours", 0, 23);
        DefensiveTools.checkRange(minutes, "minutes", 0, 59);

        int realHours = hours % 12;
        if (realHours == 0) realHours = 12;

        this.hours = realHours;
        this.minutes = minutes;
        this.pm = hours >= 12;
    }

    public Time(int hours, int minutes, Ampm ampm) {
        DefensiveTools.checkRange(hours, "hours", 1, 12);
        DefensiveTools.checkRange(minutes, "minutes", 0, 59);
        DefensiveTools.checkNull(ampm, "ampm");

        this.hours = hours;
        this.minutes = minutes;
        this.pm = ampm == PM;
    }

    public int getHours() { return hours; }

    public int getMinutes() { return minutes; }

    public Ampm getAmpm() { return pm ? PM : AM; }

    public int compareTo(Time o) {
        return getMinutesFromMidnight() - o.getMinutesFromMidnight();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Time)) return false;
        Time t = (Time) obj;

        return getAmpm() == t.getAmpm() && getHours() == t.getHours()
                && getMinutes() == t.getMinutes();
    }

    public int hashCode() {
        int val = hours ^ minutes;
        return getAmpm() == PM ? val : ~val;
    }

    public String toString() {
        String minstr = Integer.toString(minutes);
        while (minstr.length() < 2) minstr = '0' + minstr;

        return hours + ":" + minstr + (pm ? "PM" : "AM");
    }

    public int getMinutesFromMidnight() {
        int hours = this.hours;
        if (hours == 12) hours = 0;
        if (pm) hours += 12;

        return hours*60 + this.minutes;
    }

    public static enum Ampm { AM, PM }
}
