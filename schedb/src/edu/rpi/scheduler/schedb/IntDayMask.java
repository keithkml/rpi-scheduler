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

public final class IntDayMask implements DayMask {
    private int mask = 0;

    public IntDayMask() { }

    public IntDayMask(int mask) {
        this.mask = mask;
    }

    public boolean isOn(int block) {
        return block >= 0 && block < 32 && (1 << block & mask) != 0;
    }

    public boolean isEmpty() {
        return mask == 0;
    }

    public boolean add(int block) {
        return setMask(mask | (1 << block));
    }

    private boolean setMask(int newmask) {
        int old = this.mask;
        this.mask = newmask;
        return newmask != old;
    }

    public boolean delete(int block) {
        return setMask(mask & ~(1 << block));
    }

    public boolean fill() {
        return setMask(~0);
    }

    public boolean clear() {
        return setMask(0);
    }

    public boolean fitsInto(DayMask other) {
        if (other instanceof IntDayMask) {
            IntDayMask rdm = (IntDayMask) other;
            int othermask = rdm.mask;
            return (othermask & ~mask) == othermask;
        } else {
            throw new IllegalArgumentException("Cannot compare int day mask to "
                    + other.getClass().getName());
        }
    }

    public void merge(DayMask other) {
        if (other instanceof IntDayMask) {
            IntDayMask rdm = (IntDayMask) other;
            mask |= rdm.mask;
        } else {
            throw new IllegalArgumentException("Cannot compare int day mask to "
                    + other.getClass().getName());
        }
    }

    public int getMaxBlockNum() { return 31; }

    public int getTimeBlockSum() {
        int time = 0;
        int mask = this.mask;
        if (mask != 0) {
            for (int b = 0; b < 32; b++) {
                if ((1 << b & mask) != 0) time++;
            }
        }
        return time;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IntDayMask)) return false;
        IntDayMask rdm = (IntDayMask) obj;
        return mask == rdm.mask;
    }

    public int hashCode() {
        return mask;
    }
}
