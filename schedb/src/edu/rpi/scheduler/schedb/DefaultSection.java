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

import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.SectionID;
import edu.rpi.scheduler.schedb.spec.SectionNumber;
import edu.rpi.scheduler.schedb.spec.TimeRepresentation;

import java.util.Collection;

public class DefaultSection implements Section {
    private final SchedulerDataPlugin plugin;
    private final SectionNumber number;
    private final SectionID secid;
    private final Collection<ClassPeriod> periods;
    private final Notes notes;
    private final int seats;
    private WeekMask<?> weekMask = null;

    public DefaultSection(SchedulerDataPlugin plugin, SectionNumber number, SectionID secid,
            int seats, Collection<ClassPeriod> periods, Notes notes) {
        this.plugin = plugin;
        this.number = number;
        this.secid = secid;
        this.seats = seats;
        this.periods = periods;
        this.notes = notes;
    }

    public SectionNumber getNumber() { return number; }

    public SectionID getID() { return secid; }

    public int getSeats() { return seats; }

    public Collection<ClassPeriod> getPeriods() { return periods; }

    /**
     * NOTE: returned object is mutable
     */
    public WeekMask<?> getWeekMask() {
        if (weekMask == null) {
            TimeRepresentation timeRep = plugin.getTimeRepresentation();
            weekMask = timeRep.newWeekMask();
            for (ClassPeriod period : periods) {
                weekMask.merge(timeRep.getWeekMask(period.getTimePeriod()));
            }
        }

        return weekMask;
    }

    public Notes getNotes() { return notes; }

    public int compareTo(Section o) {
        return getNumber().compareTo(o.getNumber());
    }

    public String toString() {
        return getID() + ": section " + getNumber();
    }
}
