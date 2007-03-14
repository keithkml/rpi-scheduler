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

import org.jdom.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser implements JdomExternalizableMod {
    //0         ,-10      ,-30      ,-40      ,-50      ,-60      ,-70      ,-80      ,-90      ,-100     ,-110     ,-120     ,-130     ,-140
    //01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
    //  90588 ADMN-1010-01 ORAL COMM FOR TA'S I      LEC 0    SU M  R    8:00  9:20AM Steigler                                 15   0    15

    //5 12 24 3 4 2 5 5 7 21 17 4 4 4

    private static final Pattern courseLineRE = Pattern.compile(
            // "  90588 ADMN-1010-01 ORAL COMM FOR TA'S I" (groups 1-5)
            "..(.{5}).(.{4}).(.{4}).(.{2}).(.{25})."
            // "LEC 0    SU M  R    8:00  9:20AM" (6-12)
            + "(.{3}).(.{4}).(.{2}).(.{7})(.{5}).(.{5})(.{2})."
            // "Steigler                                 15   0    15" (13-17)
            + "(?:(.{21}).(.{17}).(.{4}).(.{4}).(.{1,4})|(.{0,21})).*"
    );

    private static final Pattern noteRE = Pattern.compile(".*NOTE: (.*)");

    private static final int G_CRN = 1;
    private static final int G_DEPT = 2;
    private static final int G_COURSENUM = 3;
    private static final int G_SECNUM = 4;
    private static final int G_COURSENAME = 5;

    private static final int G_PERIODTYPE = 6;
    private static final int G_CREDITS = 7;
    private static final int G_GRADETYPE = 8;
    private static final int G_DAYS = 9;
    private static final int G_FROM = 10;
    private static final int G_TO = 11;
    private static final int G_AMPM = 12;

    private static final int G_PROFA = 13;
    private static final int G_LOCATION = 14;
    private static final int G_SEATS = 15;
    private static final int G_SEATSUSED = 16;
    private static final int G_SEATSLEFT = 17;
    private static final int G_PROFB = 18;

    private List<Department> depts = new ArrayList<Department>();

    private Department dept = null;
    private Course course = null;
    private Section section = null;
    private Matcher matcher = null;
    private String line = null;

    public void parse(Reader in) {
        BufferedReader br = new BufferedReader(in);
        try {
            while (true) {
                String line = br.readLine();

                if (line == null) break;

                this.line = line;

                if (!processDataLine()) {
                    // maybe it's a note
                    processNoteLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean processDataLine() {
        matcher = courseLineRE.matcher(line);
        System.out.println("pasrsing line " + line);

        if (!matcher.matches()) return false;


        String idstr = getString(G_CRN);
        if (idstr.length() == 0) {
            // the first column contains only whitespace, so it's a blank line
            // or another period for a previous section

            // if we haven't ready any courses yet, stop parsing this line
            if (section == null) return false;

            // if there's a starting time value, it's probably another period
            // for the current section
            if (getString(G_TO).length() != 0) {
                processPeriod();
            }
        } else {
            try {
                Integer.parseInt(idstr);
            } catch (NumberFormatException e) {
                return false;
            }
            processDept();
            processCourse();
            processSection();
            processPeriod();
        }

        return true;
    }

    private void processNoteLine() {
        if (course == null) return;

        Matcher m = noteRE.matcher(line);
        if (!m.matches()) return;

        course.addNote(m.group(1).trim());
    }

    private void processDept() {
        String deptAbbrev = getString(G_DEPT);
        if (dept == null || !deptAbbrev.equals(dept.getAbbrev())) {
            dept = new Department(deptAbbrev, getDeptName(deptAbbrev));
            course = null;
            section = null;

            depts.add(dept);
        }
    }

    private Map<String,String> deptNames = new HashMap<String, String>();

    {
        deptNames.put("ARCH", "Architecture");
        deptNames.put("LGHT", "Lighting");
        deptNames.put("BMED", "Biomedical Engr.");
        deptNames.put("CHME", "Chemical Engr.");
        deptNames.put("COGS", "Cognitive Science");
        deptNames.put("CIVL", "Civil Engr.");
        deptNames.put("DSES", "Decision Sciences and Engr. Systems");
        deptNames.put("ECSE", "Electrical, Computer, and Systems Engr.");
        deptNames.put("ENGR", "General Engineering");
        deptNames.put("EPOW", "Electrical Power Engr.");
        deptNames.put("ENVE", "Environmental Engr.");
        deptNames.put("ESCI", "Engineering Science");
        deptNames.put("MANE", "Mechanical, Aerospace, and Nuclear Engr.");
        deptNames.put("MTLE", "Materials Science and Engr.");
        deptNames.put("ARTS", "Arts");
        deptNames.put("COMM", "Communication");
        deptNames.put("IHSS", "Interdisciplinary H&SS");
        deptNames.put("LANG", "Foreign Languages and Literature");
        deptNames.put("LITR", "Literature");
        deptNames.put("PHIL", "Philosophy");
        deptNames.put("STSH", "Science and Technology Studies");
        deptNames.put("WRIT", "Writing");
        deptNames.put("ECON", "Economics");
        deptNames.put("PSYC", "Psychology");
        deptNames.put("STSS", "Science and Technology Studies");
        deptNames.put("ITEC", "Information Tech.");
        deptNames.put("MGMT", "Management");
        deptNames.put("ASTR", "Astronomy");
        deptNames.put("BCBP", "Biochemistry and Biophysics");
        deptNames.put("BIOL", "Biology");
        deptNames.put("CHEM", "Chemistry");
        deptNames.put("CISH", "Computer Science at Hartford");
        deptNames.put("CSCI", "Computer Science");
        deptNames.put("ISCI", "Interdisciplinary Science");
        deptNames.put("ERTH", "Earth and Environmental Science");
        deptNames.put("MATH", "Mathematics");
        deptNames.put("MATP", "Math Prog., Prob., and Stats.");
        deptNames.put("PHYS", "Physics");
        deptNames.put("IENV", "Interdisciplinary Environmental");
        deptNames.put("USAF", "Air Force ROTC");
        deptNames.put("USAR", "Army ROTC");
        deptNames.put("USNA", "Navy ROTC");
        deptNames.put("NSST", "Natural Science for School Teachers");

        deptNames.put("ADMN", "Administration");
    }

    private String getDeptName(String deptAbbrev) {
        return deptNames.get(deptAbbrev);
    }

    private void processCourse() {
        int courseid = getInt(G_COURSENUM);

        if (course == null || (courseid != -1 && courseid != course.getNumber())) {
            String name = getString(G_COURSENAME);
            IntRange credits = getIntRange(G_CREDITS);
            if (credits == null) return;
            int seats = getInt(G_SEATS);
            GradeType type = GradeType.getType(getString(G_GRADETYPE));
            if (type == null) return;
            course = new Course(courseid, name, credits, type, seats);
            dept.addCourse(course);
        }
    }

    private IntRange getIntRange(int g) {
        return IntRange.parseRange(getString(g));
    }

    // TOSMALL: handle TBA courses
    // 90634 CSCI-6980-01 MASTER'S PROJECT          LEC 1-9            ** TBA **     Zaki
    private void processSection() {
        if (course == null) return;

        int crn = getInt(G_CRN);
        int secnum = getInt(G_SECNUM);
        int seats = getInt(G_SEATS);
        String location = getString(G_LOCATION);
        if (location != null && location.length() == 0) location = null;
        section = new Section(crn, secnum, location, seats);
        course.addSection(section);
    }

    private void processPeriod() {
        String typestr = getString(G_PERIODTYPE);
        String daysstr = matcher.group(G_DAYS);
        String startstr = getString(G_FROM);
        String endstr = getString(G_TO);
        String endpm = getString(G_AMPM);
        String prof = getString(G_PROFA);
        if (prof == null) prof = getString(G_PROFB);

        boolean[] days = new boolean[7];
        for (int i = 0; i < days.length; i++) {
            days[i] = !Character.isWhitespace(daysstr.charAt(i));
        }

        Time[] times = Time.getRange(startstr, endstr, endpm);

        if (times == null) return;

        PeriodType type = PeriodType.getType(typestr);
        // type can only be null if the type string is empty
        if (type == null) {
            if (typestr.trim().length() == 0) type = PeriodType.UNKNOWN;
            else return;
        }

        section.addPeriod(new Period(type, days, times[0], times[1], prof));
    }

    private int getInt(int g) {
        String s = matcher.group(g).trim();
        // to fix a silly bug
        s = s.replace('O', '0');
      try {
        return s == null ? -1 : Integer.parseInt(s);
      } catch (NumberFormatException e) {
        System.err.println("Warning: Couldn't parse integer " + s);
        return -1;
      }
    }

    private String getString(int g) {
        String s = matcher.group(g);
        return s == null ? null : s.trim();
    }

    public List<Department> getDepts() { return depts; }

    public void writeExternalMod(Element element) {
        for (Department dept : depts) dept.writeExternal(element);
    }
}
