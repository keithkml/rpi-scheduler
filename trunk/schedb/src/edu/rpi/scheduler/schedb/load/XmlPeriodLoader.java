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

package edu.rpi.scheduler.schedb.load;

import edu.rpi.scheduler.schedb.DefaultClassPeriod;
import edu.rpi.scheduler.schedb.DefaultPeriodTypes;
import edu.rpi.scheduler.schedb.NamedLocation;
import edu.rpi.scheduler.schedb.NamedProfessor;
import edu.rpi.scheduler.schedb.Time;
import edu.rpi.scheduler.schedb.load.spec.DataElement;
import edu.rpi.scheduler.schedb.load.spec.PeriodLoader;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Location;
import edu.rpi.scheduler.schedb.spec.PeriodType;
import edu.rpi.scheduler.schedb.spec.Professor;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import org.w3c.dom.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlPeriodLoader implements PeriodLoader {
    /*
    <period type="lecture" professor="Barthel" days="tue,fri" starts="8:00AM"
    ends="9:20AM" />
    */
    public ClassPeriod loadPeriod(
            DataElement element) throws DbLoadException {
        XmlDataElement xel = (XmlDataElement) element;

        Element perel = xel.getDomElement();

        PeriodType type = readType(perel);
        Location location = readLocation(perel);
        Professor prof = readProfessor(perel);
        boolean[] days = readDays(perel);
        Time start = readStart(perel);
        Time end = readEnd(perel);

        DefaultClassPeriod period = new DefaultClassPeriod(new DailyTimePeriod(days, start, end),
                        type, location, prof);
        readCustomData(perel, period);
        return period;
    }

    protected void readCustomData(Element perel, ClassPeriod period) {
    }

    protected PeriodType readType(Element perel) throws DbLoadException {
        String typestr = readString(perel, "type");
        if (typestr == null) return null;

        return getType(typestr);
    }

    protected PeriodType getType(String typestr) throws DbLoadException {
        String ntypestr = typestr.trim().toLowerCase();
        if (ntypestr.equals("ind-study")) return DefaultPeriodTypes.INDSTUDY;
        else if (ntypestr.equals("lab")) return DefaultPeriodTypes.LAB;
        else if (ntypestr.equals("lecture")) return DefaultPeriodTypes.LECTURE;
        else if (ntypestr.equals("recitation")) return DefaultPeriodTypes.RECITATION;
        else if (ntypestr.equals("seminar")) return DefaultPeriodTypes.SEMINAR;
        else if (ntypestr.equals("studio")) return DefaultPeriodTypes.STUDIO;
        else throw new DbLoadException("invalid period type " + ntypestr);
    }

    protected Location readLocation(Element perel) throws DbLoadException {
        String locstr = readString(perel, "location");
        return locstr == null ? null : new NamedLocation(locstr);
    }

    protected Professor readProfessor(Element perel) throws DbLoadException {
        String profstr = readString(perel, "professor");
        return profstr == null ? null : new NamedProfessor(profstr);
    }

    protected String readString(Element perel, String attrname)
            throws DbLoadException {
        if (!perel.hasAttribute(attrname)) return null;
        return perel.getAttribute(attrname);
    }

    protected boolean[] readDays(Element perel) throws DbLoadException {
        String daysstr = readString(perel, "days");
        if (daysstr == null) return null;

        return readDays(daysstr);
    }

    protected boolean[] readDays(String daysstr) throws DbLoadException {
        boolean[] days = new boolean[7];
        // this regular expression allows things like "wed-thur-fri",
        // "wednesday thursday friday", "wed/thu/fri", or the preferred
        // "wed,thu,fri"
        int valid = 0;
        String[] daystrs = daysstr.split("[,-/+ ]+");
        for (final String newVar : daystrs) {
            String daystr = newVar.toLowerCase().trim();
            if (daystr.length() == 0) continue;

            // this matches thing like:
            //   "mon" for monday
            //   "t" for tuesday
            //   "tr" for thursday
            //   "thur" for thursday
            //   "su" for sunday
            // note that "s" doesn't match anything, neither saturday nor sunday
            if ("monday".startsWith(daystr)) {
                days[0] = true;
            } else if ("tuesday".startsWith(daystr)) {
                days[1] = true;
            } else if ("wednesday".startsWith(daystr)) {
                days[2] = true;
            } else if ("thursday".startsWith(daystr)
                    || daystr.equals("tr") || daystr.equals("r")) {
                days[3] = true;
            } else if ("friday".startsWith(daystr)) {
                days[4] = true;
            } else if (daystr.length() >= 2 && "saturday".startsWith(daystr)) {
                days[5] = true;
            } else if (daystr.length() >= 2 && "sunday".startsWith(daystr)) {
                days[6] = true;
            } else {
                throw new DbLoadException("invalid day name '" + daystr
                        + "' (part of day list '" + daysstr + "')");
            }
            valid++;
        }

        if (valid == 0 && !daysstr.trim().equals("")) {
            throw new DbLoadException("no day names found in string '"
                    + daysstr + "'");
        }

        return days;
    }

    protected Time readStart(Element perel) throws DbLoadException {
        return readTime(perel, "starts");
    }

    protected Time readEnd(Element perel) throws DbLoadException {
        return readTime(perel, "ends");
    }

    protected static final Pattern timeRE = Pattern.compile(
            "(\\d{1,2}):(\\d{2})\\s?(AM|PM)", Pattern.CASE_INSENSITIVE);

    protected Time readTime(Element perel, String attrname)
            throws DbLoadException {
        String timestr = readString(perel, attrname);
        if (timestr == null) return null;

        return readTime(timestr);
    }

    protected Time readTime(String timestr) throws DbLoadException {
        String ntimestr = timestr.trim();
        Matcher m = timeRE.matcher(ntimestr);
        if (!m.matches()) {
            throw new DbLoadException("invalid time string " + ntimestr);
        }

        String hstr = m.group(1);
        String mstr = m.group(2);
        String ampm = m.group(3);
        int hi;
        int mi;
        try {
            hi = Integer.parseInt(hstr);
            mi = Integer.parseInt(mstr);
        } catch (NumberFormatException e) {
            throw new DbLoadException("invalid time string " + ntimestr, e);
        }
        if (hi < 1 || hi > 12) {
            throw new DbLoadException("invalid hour value '" + hi + "'");
        }
        if (mi < 0 || mi > 59) {
            throw new DbLoadException("invalid minute value '" + mi + "'");
        }
        boolean pm = ampm.equalsIgnoreCase("pm");

        return new Time(hi, mi, pm ? Time.PM : Time.AM);
    }
}
