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

import edu.rpi.scheduler.schedb.spec.DayMask;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.TimePeriod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

public class DefaultSchedule implements Schedule {
    private final SchedulerDataPlugin plugin;

    private final List<UniqueSection> sections = new ArrayList<UniqueSection>();
    private final WeekMask<?> mask;
    private WeekMask<?> classMask = null;

    private int daysOff = -1;
    private int[] times = null;

    public DefaultSchedule(SchedulerDataPlugin plugin) {
        this(plugin, null);
    }

    public DefaultSchedule(SchedulerDataPlugin schedulerPlugin, Schedule schedule) {
        plugin = schedulerPlugin;
        mask = plugin.getTimeRepresentation().newWeekMask();
        if (schedule != null) {
            sections.addAll(schedule.getSections());
            mask.merge(schedule.getTimeMask());
        }
    }

    public SchedulerDataPlugin getSchedulerPlugin() { return plugin; }

    public void addSection(UniqueSection section) {
        sections.add(section);
        mask.merge(section.getTimeMask());
        reset();
    }

    public boolean canAdd(UniqueSection section) {
        WeekMask<?> mask = section.getTimeMask();
        return canAdd(mask);
    }

    public boolean canAdd(WeekMask<?> mask) {
        return mask.fitsInto(this.mask);
    }

    public WeekMask<?> getTimeMask() { return mask; }

    public List<UniqueSection> getSections() {
        return sections;
    }


    private void reset() {
        daysOff = -1;
        times = null;
//        betweens = null;
        classMask = null;
    }

    public WeekMask<?> getClassMask() {
        if (classMask == null) {
            WeekMask<?> mask = plugin.getTimeRepresentation().newWeekMask();

            for (UniqueSection section : sections) {
                mask.merge(section.getTimeMask());
            }

            classMask = mask;
        }

        return classMask;
    }

    public int getDaysOfClass() {
        if (daysOff == -1) {
            int days = 0;
            WeekMask<?> mask = getClassMask();

            for (int i = 0; i < 7; i++) {
                if (!mask.getDayMask(i).isEmpty()) days++;
            }

            daysOff = days;
        }

        return daysOff;
    }

    public int[] getTimeSums() {
        if (times == null) {
            int[] halves = new int[7];
            WeekMask<?> mask = getClassMask();

            for (int i = 0; i < 7; i++) {
                DayMask day = mask.getDayMask(i);

                int time = day.getTimeBlockSum();

                halves[i] = time;
            }

            Arrays.sort(halves);
            // reverse the sorting
            for (int i = 0; i < halves.length/2; i++) {
                int j = halves.length-i-1;
                int temp = halves[i];
                halves[i] = halves[j];
                halves[j] = temp;
            }
            times = halves;
        }

        return times;
    }

    public List<List<TimePeriod>> getBetweens() {
        List<List<TimePeriod>> periodsByDay = new ArrayList<List<TimePeriod>>(7);
        for (int i = 0; i < 7; i++) {
            periodsByDay.add(new ArrayList<TimePeriod>());
        }
        for (UniqueSection section : sections) {
            Collection<SectionDescriptor> sectionDescriptors = section.getSectionDescriptors();
            // we only need to look at the first section descriptor, since the
            // rest are part of the same unique section, so they have the same
            // class periods
            SectionDescriptor sd = sectionDescriptors.iterator().next();
            for (ClassPeriod period : sd.getActualSection().getPeriods()) {
                DailyTimePeriod tp = period.getTimePeriod();
                int day = 0;
                for (boolean on : tp.getDays()) {
                    if (on) periodsByDay.get(day).add(tp.getPeriod());
                    day++;
                }
            }
        }

        List<List<TimePeriod>> betweens = new ArrayList<List<TimePeriod>>(7);
        for (int i = 0; i < 7; i++) {
            betweens.add(new ArrayList<TimePeriod>(5));
        }
        int d = 0;
        for (List<TimePeriod> periods : periodsByDay) {
            Collections.sort(periods);
            for (int i = 1; i < periods.size(); i++) {
                TimePeriod first = periods.get(i-1);
                TimePeriod second = periods.get(i);
                betweens.get(d).add(TimePeriod.getPeriodBetween(first, second));
            }
            d++;
        }
        return betweens;
    }
}
