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

public class TimePeriod implements Comparable<TimePeriod> {
    public static TimePeriod getPeriodBetween(TimePeriod first, TimePeriod second) {
        if (first.getStart().compareTo(second.getStart()) > 0) {
            TimePeriod tmp = first;
            first = second;
            second = tmp;
        }
        if (first.getEnd().compareTo(second.getStart()) > 0) {
            throw new IllegalArgumentException("periods overlap: " + first
                    + " and " + second);
        }
        return new TimePeriod(first.getEnd(), second.getStart());
    }

    private final Time start;
    private final Time end;

    public TimePeriod(Time start, Time end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("start (" + start + ") is after "
                    + "end (" + end + ")");
        }

        this.start = start;
        this.end = end;
    }

    public Time getStart() { return start; }

    public Time getEnd() { return end; }

    public int getElapsedMinutes() {
        return end.getMinutesFromMidnight() - start.getMinutesFromMidnight();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyTimePeriod)) return false;

        final DailyTimePeriod other = (DailyTimePeriod) o;

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
        return getStart() + "-" + getEnd();
    }

    public int compareTo(TimePeriod other) {
        int result = getStart().compareTo(other.getStart());
        if (result != 0) return result;
        return getEnd().compareTo(other.getEnd());
    }
}
