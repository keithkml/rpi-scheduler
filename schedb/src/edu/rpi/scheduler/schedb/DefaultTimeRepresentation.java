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
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.DayMask;
import edu.rpi.scheduler.schedb.spec.TimeRepresentation;
import static java.lang.Math.abs;

public class DefaultTimeRepresentation implements TimeRepresentation {
    private static final int MINS_PER_BLOCK = 5;
    private static final int LAST_BLOCK = ((24*60)/MINS_PER_BLOCK)-1;

    public WeekMask<BitSetDayMask> newWeekMask() {
        return WeekMask.newMask(BitSetDayMask.class);
    }

    protected int getBlockFromMins(int mins) {
        return getNormalizedBlock(mins / MINS_PER_BLOCK);
    }

    private static int getNormalizedBlock(int block) {
        if (block < 0) {
            block = 0;
        } else {
            int max = getMaxBlockNumber();
            if (block > max) block = max;
        }
        return block;
    }

    private static int getMaxBlockNumber() { return LAST_BLOCK; }

    protected int getMinsFromBlock(int block) {
        return getNormalizedBlock(block) * MINS_PER_BLOCK;
    }

    public Time getTime(int block) {
        int mins = getMinsFromBlock(block);
        int hours = mins / 60;
        int minsInHour = mins % 60;
        return new Time(hours, minsInHour);
    }

    public Duration getDuration(int blocks) {
        return new Duration(getMinsFromBlock(blocks));
    }

    public int getClosestBlock(Time time, Bias bias) {
        DefensiveTools.checkNull(time, "time");
        DefensiveTools.checkNull(bias, "bias");

        int givenMinutes = time.getMinutesFromMidnight();
        int blocks = getBlockFromMins(givenMinutes);
        Time computedTime = getTime(blocks);

        if (computedTime.equals(time)) {
            return blocks;

        } else if (bias == Bias.EARLIER) {
            assert computedTime.compareTo(time) <= 0;
            return blocks;

        } else if (bias == Bias.LATER) {
            blocks++;
            computedTime = getTime(blocks);
            assert computedTime.compareTo(time) >= 0;
            return blocks;

        } else if (bias == Bias.CLOSEST) {
            Time computedAfter = getTime(blocks + 1);
            int earlierDiff = abs(computedTime.getMinutesFromMidnight()
                    - givenMinutes);
            int laterDiff = abs(computedAfter.getMinutesFromMidnight()
                    - givenMinutes);

            return laterDiff > earlierDiff ? blocks + 1 : blocks;
        }
        throw new IllegalStateException("could not find closest block: " + time
                + " (" + bias + ")");
    }

    public WeekMask<? extends DayMask> getWeekMask(DailyTimePeriod time) {
        WeekMask<? extends DayMask> mask = newWeekMask();
        Time start = time.getStart();
        Time end = time.getEnd();

        int first = getClosestBlock(start, TimeRepresentation.Bias.EARLIER);
        int last = getClosestBlock(end, TimeRepresentation.Bias.EARLIER);

        boolean[] days = time.getDays();
        for (int i = 0; i < 7; i++) {
            if (days[i]) addBits(mask.getDayMask(i), first, last);
        }
        return mask;
    }

    private static void addBits(DayMask mask, int first, int last) {
        for (int i = first; i <= last; i++) mask.add(i);
    }
}
