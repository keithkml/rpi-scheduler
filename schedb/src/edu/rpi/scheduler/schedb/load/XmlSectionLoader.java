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

import edu.rpi.scheduler.schedb.DefaultSection;
import edu.rpi.scheduler.schedb.IntSectionNumber;
import edu.rpi.scheduler.schedb.StringSectionID;
import edu.rpi.scheduler.schedb.load.spec.DataElement;
import edu.rpi.scheduler.schedb.load.spec.NotesLoader;
import edu.rpi.scheduler.schedb.load.spec.PeriodLoader;
import edu.rpi.scheduler.schedb.load.spec.SectionLoader;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Notes;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.schedb.spec.SectionID;
import edu.rpi.scheduler.schedb.spec.SectionNumber;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.SECTION;

public class XmlSectionLoader extends XmlLoader implements SectionLoader {
    private static final Logger logger
            = Logger.getLogger(XmlSectionLoader.class.getPackage().getName());

    public XmlSectionLoader(MutableDataLoadingContext context) {
        super(context);
    }

    public Section loadSection(DataElement element)
            throws DbLoadException {
        XmlDataElement xel = (XmlDataElement) element;

        Element secel = xel.getDomElement();

        SectionNumber number = readNumber(secel);
        SectionID secid = readSecId(secel);

        String use = null;
        if (number != null) use = number.toString();
        else if (secid != null) use = secid.toString();
        if (use != null) {
            getContext().fireLoadingObject(SECTION, use);
        }

        int seats = readSeats(secel);
        List<ClassPeriod> periods = readPeriods(secel);
        Notes notes = readNotes(secel);
        DefaultSection section = createSection(number, secid, seats, periods,
                notes);
        readCustomData(secel, section);

        return section;
    }

    protected void readCustomData(Element secel, Section section) {
    }

    private int readSeats(Element secel) throws DbLoadException {
        String numstr = getString(secel, "seats");
        return numstr == null ? -1 : Integer.parseInt(numstr);
    }

    /*
    <section crn="90589" number="2">
    <period type="lecture" professor="Barthel" days="tue,fri" starts="8:00AM"
    ends="9:20AM" />
    </section>
    */
    protected DefaultSection createSection(SectionNumber number,
            SectionID secid, int seats, List<ClassPeriod> periods, Notes notes) {
        return new DefaultSection(getContext().getPlugin(), number, secid, seats, periods, notes);
    }

    protected String getString(Element secel, String attrname)
            throws DbLoadException {
        if (!secel.hasAttribute(attrname)) return null;
        else return secel.getAttribute(attrname);
    }

    protected SectionNumber readNumber(Element coursel)
            throws DbLoadException {
        String numstr = getString(coursel, "number");
        return numstr == null
                ? null : new IntSectionNumber(Integer.parseInt(numstr));
    }

    protected SectionID readSecId(Element coursel) throws DbLoadException {
        String idstr = getString(coursel, "crn");
        return idstr == null ? null : new StringSectionID(idstr);
    }

    protected List<ClassPeriod> readPeriods(Element coursel) throws DbLoadException {
        List<ClassPeriod> periods = new ArrayList<ClassPeriod>();
        NodeList perels = coursel.getElementsByTagName("period");
        for (int i = 0; i < perels.getLength(); i++) {
            Element perel = (Element) perels.item(i);
            try {
                periods.add(readPeriod(perel));
            } catch (DbLoadException e) {
                logger.log(Level.WARNING, "Error reading period", e);
                continue;
            }
        }
        return periods;
    }

    /*
    <period type="lecture" professor="Steigler" days="tue,fri" starts="2:00PM"
    ends="3:20PM" />

    */
    protected ClassPeriod readPeriod(Element perel) throws DbLoadException {
        PeriodLoader loader = getContext().getPeriodLoader();
        return loader.loadPeriod(new XmlDataElement(perel));
    }

    protected Notes readNotes(Element coursel) throws DbLoadException {
        NotesLoader loader = getContext().getNotesLoader();
        return loader.readNotes(new XmlDataElement(coursel));
    }
}
