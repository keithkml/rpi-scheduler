/*
 *  Copyright (c) 2003, The Joust Project
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
 *  - Neither the name of the Joust Project nor the names of its 
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
 *  File created by keith @ Dec 6, 2003
 *
 */

package edu.rpi.scheduler.schedb.load;

import edu.rpi.scheduler.schedb.DefaultPeriodTypes;
import edu.rpi.scheduler.schedb.Time;
import junit.framework.TestCase;

import java.util.Arrays;

public class SelfTest extends TestCase {
    public void testXmlPeriodLoader() throws DbLoadException {
        XmlPeriodLoader xsl = new XmlPeriodLoader();
        assertSame(DefaultPeriodTypes.INDSTUDY, xsl.getType("ind-study"));
        assertSame(DefaultPeriodTypes.LAB, xsl.getType("lab"));
        assertSame(DefaultPeriodTypes.LECTURE, xsl.getType("lecture"));
        assertSame(DefaultPeriodTypes.RECITATION, xsl.getType("recitation"));
        assertSame(DefaultPeriodTypes.SEMINAR, xsl.getType("seminar"));
        assertSame(DefaultPeriodTypes.STUDIO, xsl.getType("studio"));

        try {
            xsl.getType(null);
            fail();
        } catch (Throwable t) {
            // something should be thrown
        }

        try {
            xsl.getType("nothing");
            fail();
        } catch (DbLoadException t) {
            // this should be thrown
        }

        boolean[] days = xsl.readDays("wed,mon,thu");
        assertTrue(Arrays.equals(days, new boolean[] {
            true, false, true, true, false, false, false}));

        days = xsl.readDays("- t  -  r -- f    -");
        assertTrue(Arrays.equals(days, new boolean[] {
            false, true, false, true, true, false, false}));

        try {
            xsl.readDays("");
            fail();
        } catch (DbLoadException e) {
            // this should be thrown
        }

        Time time = xsl.readTime("4:13PM");
        assertEquals(4, time.getHours());
        assertEquals(13, time.getMinutes());
        assertTrue(time.getAmpm() == Time.PM);

        time = xsl.readTime("12:09am");
        assertEquals(12, time.getHours());
        assertEquals(9, time.getMinutes());
        assertFalse(time.getAmpm() == Time.PM);

        time = xsl.readTime(" 12:09 am ");
        assertEquals(12, time.getHours());
        assertEquals(9, time.getMinutes());
        assertFalse(time.getAmpm() == Time.PM);

        try {
            xsl.readTime("12:09  AM");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("2:61AM");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("0:20AM");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("13:04AM");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("-1:-1PM");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("2 : 02 PM");

            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("13:02");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime("12:2");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }

        try {
            xsl.readTime(":44");
            fail();
        } catch (DbLoadException e) {
            // should be thrown
        }
    }
}
