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

import java.util.BitSet;

public final class BitSetDayMask implements DayMask {
    public static final int MAX_BLOCK_NUM = 1440;

    private BitSet mask = new BitSet(MAX_BLOCK_NUM);

    public BitSetDayMask() { }

    public BitSetDayMask(BitSet mask) {
        if (mask.length() != MAX_BLOCK_NUM) {
            throw new IllegalArgumentException("given BitSet mask has "
                    + mask.length() + " bits, but we need " + MAX_BLOCK_NUM
                    + " bits");
        }
        this.mask.clear();
        this.mask.or(mask);
    }

    public boolean isOn(int block) {
        return mask.get(block);
    }

    public boolean isEmpty() {
        return mask.isEmpty();
    }

    public boolean add(int block) {
        boolean wasOn = mask.get(block);
        mask.set(block);
        return wasOn;
    }

    public boolean delete(int block) {
        boolean wasOn = mask.get(block);
        mask.clear(block);
        return wasOn;
    }

    public boolean fill() {
        if (mask.cardinality() == mask.length()) return false;
        mask.set(0, mask.length() - 1, true);
        return true;
    }

    public boolean clear() {
        if (mask.cardinality() == 0) return false;
        mask.clear();
        return true;
    }

    public boolean fitsInto(DayMask other) {
        if (other instanceof BitSetDayMask) {
            BitSetDayMask rdm = (BitSetDayMask) other;
            return !rdm.mask.intersects(mask);
        } else {
            throw new IllegalArgumentException("Cannot compare BitSet day mask to "
                    + other.getClass().getName());
        }
    }

    public void merge(DayMask other) {
        if (other instanceof BitSetDayMask) {
            BitSetDayMask rdm = (BitSetDayMask) other;
            mask.or(rdm.mask);
        } else {
            throw new IllegalArgumentException("Cannot compare int day mask to "
                    + other.getClass().getName());
        }
    }

    public int getMaxBlockNum() { return MAX_BLOCK_NUM; }

    public int getTimeBlockSum() {
        return mask.cardinality();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BitSetDayMask)) return false;
        BitSetDayMask rdm = (BitSetDayMask) obj;
        return mask.equals(rdm.mask);
    }

    public int hashCode() {
        return mask.hashCode();
    }
}
