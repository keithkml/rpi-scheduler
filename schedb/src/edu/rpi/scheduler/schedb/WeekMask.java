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

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class WeekMask<DM extends DayMask> {
    public static <DM extends DayMask> WeekMask<DM> newMask(Class<DM> cl) {
        try {
            return new WeekMask<DM>(cl);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("cannot create new "
                    + cl.getName(), e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("cannot create new "
                    + cl.getName(), e);
        }
    }

    private final List<DM> days;

    private WeekMask(Class<DM> cl) throws IllegalAccessException,
            InstantiationException {
        List<DM> days = new ArrayList<DM>(7);
        for (int i = 0; i < 7; i++) {
            days.add(cl.newInstance());
        }
        this.days = days;
    }

    public WeekMask(WeekMask<? extends DM> other) {
        this(other.days);
    }

    public WeekMask(Collection<? extends DM> mask) {
        if (mask.size() != 7) {
            throw new IllegalArgumentException("array is not of length 7 " +
                    "(it's " + mask.size() + ")");
        }
        this.days = new ArrayList<DM>(mask);

        assert days.size() == 7;
    }

    public final boolean isOn(int day, int block) {
        return days.get(day).isOn(block);
    }

    public final boolean isEmpty() {
        for (DayMask mask : days) {
            if (!mask.isEmpty()) return false;
        }
        return true;
    }

    public final boolean isEmpty(int day) {
        return days.get(day).isEmpty();
    }

    public final boolean add(int day, int block) {
        return days.get(day).add(block);
    }

    public final boolean delete(int day, int block) {
        return days.get(day).delete(block);
    }

    public final boolean fill(int day) {
        return days.get(day).fill();
    }

    public final boolean clear(int day) {
        return days.get(day).clear();
    }

    public final boolean fitsInto(WeekMask<?> other) {
        for (int i = 0; i < 7; i++) {
            if (!days.get(i).fitsInto(other.getDayMask(i))) return false;
        }

        return true;
    }

    public final void merge(WeekMask<?> other) {
        for (int i = 0; i < 7; i++) {
            days.get(i).merge(other.getDayMask(i));
        }
    }

    public DM getDayMask(int day) {
        return days.get(day);
    }

    public int getTimeBlockSum() {
        int time = 0;
        for (DayMask day : days) time += day.getTimeBlockSum();

        return time;
    }

    public final boolean equals(Object other) {
        if (!(other instanceof WeekMask)) return false;

        return days.equals(((WeekMask<?>) other).days);
    }

    public int hashCode() {
        int code = 0;
        for (DayMask mask : days) code ^= mask.hashCode();
        return code;
    }
}
