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
package edu.rpi.scheduler.ui.ics;
/*
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.Schedule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Summary;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.JANUARY;

public class IcsExporter {
    public void export(Schedule sched) {
        ComponentList components = new ComponentList();
        // create timezone property..
        VTimeZone tz = VTimeZone.getDefault();

        // create tzid parameter..
        TzId tzParam = new TzId(tz.getProperties().getProperty(Property.TZID)
                .getValue());

        // create value parameter..
        Value type = new Value(Value.DATE);

        for (UniqueSection us : sched.getSections()) {
            // create event start date..
            java.util.Calendar startDate = java.util.Calendar.getInstance();
            startDate.set(MONTH, JANUARY);
            startDate.set(DAY_OF_MONTH, 15);

            DtStart start = new DtStart(startDate.getTime());
            start.getParameters().add(tzParam);
            start.getParameters().add(type);


            Summary summary = new Summary(us.getCourse().getName());

            VEvent event = new VEvent();
            PropertyList props = event.getProperties();
            props.add(start);
            props.add(summary);

            components.add(event);
        }
        Calendar cal = new Calendar(components);
    }
}
*/