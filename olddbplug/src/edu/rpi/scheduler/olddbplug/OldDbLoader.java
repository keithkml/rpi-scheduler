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

package edu.rpi.scheduler.olddbplug;

import edu.rpi.scheduler.schedb.DefaultClassPeriod;
import edu.rpi.scheduler.schedb.DefaultCourse;
import edu.rpi.scheduler.schedb.DefaultDepartment;
import edu.rpi.scheduler.schedb.DefaultSchedulerData;
import edu.rpi.scheduler.schedb.DefaultSection;
import edu.rpi.scheduler.schedb.IntSectionNumber;
import edu.rpi.scheduler.schedb.NamedProfessor;
import edu.rpi.scheduler.schedb.StringCourseID;
import edu.rpi.scheduler.schedb.StringSectionID;
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.load.DbLoadException;
import edu.rpi.scheduler.schedb.load.MutableDataLoadingContext;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoadListener;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoader;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.CourseID;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Professor;
import edu.rpi.scheduler.schedb.spec.ResourceLoader;
import edu.rpi.scheduler.schedb.spec.SectionID;
import edu.rpi.scheduler.schedb.spec.SectionNumber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldDbLoader implements DatabaseLoader {
    /*
    ECSE 2010#01#60421#ELECTRIC CIRCUITS# carlson # 251659200 15360 0 960 0 0 0

    0101 301 #01#010130101#FINANCIAL ACCOUNTI#OLIVER# 960 0 960 0 0 0 0
    */
    private static final Pattern sectionRE = Pattern.compile(
                    "^\\s*" +
                    "\\s*(.+)\\s*#" +     // 1: ECSE
                    "\\s*(.+)\\s*#" +    // 2: 2010
                    "\\s*(.+)\\s*#" +  // 3: 01
                    "\\s*(.+)\\s*#" +  // 4: 60421
                    "\\s*(.+?)\\s*#" + // 5: ELECTRIC CIRCUITS
                    "\\s*(.+?)\\s*#" + // 6: carlson
                    "\\s*(\\d+)" +     // 7: 251659200
                    "\\s*(\\d+)" +     // 8: 15360
                    "\\s*(\\d+)" +     // 9: 0
                    "\\s*(\\d+)" +     // 10: 960
                    "\\s*(\\d+)" +     // 11: 0
                    "\\s*(\\d+)" +     // 12 :0
                    "\\s*(\\d+)" +     // 13: 0
                    "\\s*$");

    private MutableDataLoadingContext context;
    private ResourceLoader loader = null;

    public OldDbLoader(MutableDataLoadingContext context) {
        this.context = context;
    }

    private Map<String,Department> depts = new HashMap<String, Department>();

    public void setResourceLoader(ResourceLoader loader) {
        this.loader = loader;
    }

    public void loadDb(String uri, DatabaseLoadListener listener)
            throws DbLoadException {
        BufferedReader reader;
        InputStream stream = null;
        try {
            stream = loader.loadResource(uri);
            reader = new BufferedReader(new InputStreamReader(stream));
        } catch (IOException e) {
            if (stream != null) {
                try { stream.close(); } catch (IOException ignored) { }
            }
            throw new DbLoadException(e);
        }

        Map<String,Department> deptmap = depts;
        MutableDataLoadingContext context = this.context;
        try {
            DefaultCourse course = null;
            String lastDeptName = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;

                Matcher m = sectionRE.matcher(line);

                if (!m.matches()) continue;

                String deptName = m.group(1);
                String courseidstr = m.group(2);
                String secnumstr = m.group(3);
                String secidstr = m.group(4);
                String courseName = m.group(5);
                String profname = m.group(6);

                SectionNumber secnum = new IntSectionNumber(
                        Integer.parseInt(secnumstr));
                SectionID secid = new StringSectionID(secidstr);
                CourseID courseid = new StringCourseID(courseidstr);
                Professor prof = new NamedProfessor(profname);

                List<ClassPeriod> tperiods = new ArrayList<ClassPeriod>();
                for (int i = 0; i < 7; i++) {
                    int mask = Integer.parseInt(m.group(7 + i));

                    DailyTimePeriod[] tps = getPeriods(i, mask);
                    for (DailyTimePeriod tp : tps) {
                        DailyTimePeriod timePeriod = new DailyTimePeriod(
                                i, tp.getStart(), tp.getEnd());
                        tperiods.add(new DefaultClassPeriod(timePeriod, null,
                                null, prof));
                    }
                }


                // see if this is the same course as the last one, for a bit
                // of speed..
                if (course == null || !deptName.equals(lastDeptName)
                        || !course.getNumber().equals(courseid)) {
                    DefaultDepartment dept
                            = (DefaultDepartment) deptmap.get(deptName);
                    if (dept == null) {
                        dept = new DefaultDepartment(deptName);
                        deptmap.put(deptName, dept);
                    }

                    DefaultCourse nextCourse
                            = (DefaultCourse) dept.getCourse(courseid);
                    if (nextCourse == null) {
                        nextCourse = new DefaultCourse(courseName,
                                courseid, null, null, null);
                        dept.addCourse(nextCourse);
                    }

                    course = nextCourse;
                }

                course.addSection(new DefaultSection(context.getPlugin(),
                        secnum, secid, -1, tperiods, null));
            }

            DefaultSchedulerData data = (DefaultSchedulerData) context.getSchedulerDataObj();
            for (Department dept : deptmap.values()) {
                data.addCompletedDepartment(dept);
            }
        } catch (IOException e) {
            throw new DbLoadException(e);
        } finally {
            depts = null;
            try { stream.close(); } catch (IOException e1) { }
        }
    }

    private DailyTimePeriod[] getPeriods(int day, int dayMask) {
        LinkedList<DailyTimePeriod> periodList = new LinkedList<DailyTimePeriod>();
        int start = -1;
        // (we count to 32 to avoid cleanup after the loop :)
        for (int bit = 0; bit <= 32; bit++) {
            boolean on = bit != 32 && ((1 << bit & dayMask) != 0);
            if (!on && start != -1) {
                // the block ends here.
                periodList.add(new DailyTimePeriod(day,
                        getTime(start), getTime(bit)));
                start = -1;
            } else if (on && start == -1) {
                start = bit;
            }
        }

        return periodList.toArray(
                new DailyTimePeriod[periodList.size()]);
    }

    private Time getTime(int bit) {
        int halfhour = bit+(7*2);
        int hours = halfhour/2;
        int minutes = 30*(halfhour%2);
        int hours12 = hours > 12 ? hours % 12 : hours;
        boolean pm = hours >= 12;

        return new Time(hours12, minutes, pm ? Time.PM : Time.AM);
    }
}
