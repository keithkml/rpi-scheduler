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

package edu.rpi.scheduler.schedb.spec;

import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.Duration;
import edu.rpi.scheduler.DefensiveTools;

import java.util.Arrays;

public class DailyTimePeriod {
    private static final String[] DAYNAMES = new String[] {
        "mon", "tue", "wed", "thu", "fri", "sat", "sun"
    };

    public static int getColumnFromDayNumber(DailyTimePeriod tp, int day) {
        DefensiveTools.checkRange(day, "day", 0, 6);

        int realday = day;
        boolean[] days = tp.days;
        for (int i = 0; i < day; i++) {
            if (!days[i]) realday--;
        }
        return realday;
    }

    private final boolean[] days;
    private final TimePeriod period;

    public DailyTimePeriod(int day, TimePeriod period) {
        this(day, period.getStart(), period.getEnd());
    }

    public DailyTimePeriod(int day, DailyTimePeriod period) {
        this(day, period.getPeriod());
    }

    public DailyTimePeriod(int day, Time start, Time end) {
        if (day < 0 || day > 6) {
            throw new IllegalArgumentException("day must be 0-6 inclusive, but "
                    + "it was " + day);
        }

        boolean[] days = new boolean[7];
        days[day] = true;

        this.days = days;
        this.period = new TimePeriod(start, end);
    }

    public DailyTimePeriod(boolean[] odays, Time start, Time end) {
        boolean[] days = odays.clone();
        if (days.length != 7) {
            throw new IllegalArgumentException("days must contain 7 elements "
                    + "(it contains " + days.length + ")");
        }

        this.days = days;
        this.period = new TimePeriod(start, end);
    }

    public boolean isOnDay(int day) { return days[day]; }

    public boolean[] getDays() { return days.clone(); }

    public int getDayCount() {
        int count = 0;
        for (boolean b : days) if (b) count++;
        return count;
    }

    /**
     * Returns the single day that this time period is on for, or {@code -1}
     * if this time period is not on any days or if it is on for more than one
     * day.
     */
    public int getDay() {
        int day = -1;
        int d = 0;
        for (boolean today : days) {
            if (today) {
                // if the day variable has already been set, then we must
                // return -1
                if (day != -1) return -1;
                day = d;
            }
            d++;
        }
        return day;
    }

    public TimePeriod getPeriod() { return period; }

    public Time getStart() { return period.getStart(); }

    public Time getEnd() { return period.getEnd(); }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyTimePeriod)) return false;

        final DailyTimePeriod other = (DailyTimePeriod) o;

        if (!Arrays.equals(getDays(), other.getDays())) return false;
        if (!getEnd().equals(other.getEnd())) return false;
        if (!getStart().equals(other.getStart())) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getStart().hashCode();
        result = 29 * result + getEnd().hashCode();
        return result;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(7*4);
        boolean first = true;
        boolean[] days = getDays();
        for (int i = 0; i < days.length; i++) {
            if (days[i]) {
                if (!first) sb.append(",");
                else first = false;

                sb.append(DAYNAMES[i]);
            }
        }

        return getStart() + "-" + getEnd() + " on " + sb;
    }

    public Duration getDuration() {
        return new Duration(getEnd().getMinutesFromMidnight()
                - getStart().getMinutesFromMidnight());
    }
}
