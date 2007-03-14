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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IntRange {
    private static final Pattern rangeRE = Pattern.compile("(\\d+)(?:-(\\d+))?");

    public static IntRange parseRange(String str) {
        Matcher m = rangeRE.matcher(str);
        if (!m.matches()) return null;

        int from = Integer.parseInt(m.group(1));
        String tostr = m.group(2);
        if (tostr == null) return new IntRange(from);
        else return new IntRange(from, Integer.parseInt(tostr));
    }

    private final int from;
    private final int to;

    public IntRange(int from) {
        this.from = from;
        this.to = -1;
    }

    public IntRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public boolean isRange() { return to != -1; }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String toString() {
        return "" + from + (isRange() ? "-" + to : "");
    }
}
