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

import java.util.Map;
import java.util.HashMap;

public class PeriodType {
    private static final Map<String,PeriodType> types = new HashMap<String, PeriodType>();

    public static final PeriodType LAB = new PeriodType("LAB");
    public static final PeriodType LECTURE = new PeriodType("LEC");
    public static final PeriodType RECITATION = new PeriodType("REC");
    public static final PeriodType STUDIO = new PeriodType("STU");
    public static final PeriodType SEMINAR = new PeriodType("SEM");
    public static final PeriodType INDSTUDY = new PeriodType("IND");
    public static final PeriodType UNKNOWN = new PeriodType("");

    private static synchronized void register(PeriodType type) {
        String name = type.getName();
        types.put(name, type);
    }

    public static synchronized PeriodType getType(String name) {
        return types.get(name);
    }

    private final String name;

    private PeriodType(String name) {
        this.name = name;
        register(this);
    }

    public String getName() { return name; }

    public String toString() {
        return name;
    }
}
