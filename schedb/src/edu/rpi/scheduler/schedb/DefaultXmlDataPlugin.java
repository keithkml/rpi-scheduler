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

import edu.rpi.scheduler.schedb.load.DefaultDataLoadingContext;
import edu.rpi.scheduler.schedb.load.XmlCourseLoader;
import edu.rpi.scheduler.schedb.load.XmlDeptLoader;
import edu.rpi.scheduler.schedb.load.XmlDatabaseLoader;
import edu.rpi.scheduler.schedb.load.XmlNotesLoader;
import edu.rpi.scheduler.schedb.load.XmlPeriodLoader;
import edu.rpi.scheduler.schedb.load.XmlSectionLoader;
import edu.rpi.scheduler.schedb.spec.DataContext;

public class DefaultXmlDataPlugin extends XmlSchedulerDataPlugin {
    public String getVersionString() {
        return "4.0";
    }

    public String getName() {
        return "XML Course Database Plugin";
    }

    public DataContext newSchedulingContext() {
        DefaultDataContext context = new DefaultDataContext(this);
        DefaultDataLoadingContext loadContext
                = new DefaultDataLoadingContext(this, context);
        context.setDataLoadingContext(loadContext);
        loadContext.setSchedulerDataObj(new DefaultSchedulerData(context));
        loadContext.setFileLoader(new XmlDatabaseLoader(context));
        loadContext.setDeptLoader(new XmlDeptLoader(loadContext));
        loadContext.setCourseLoader(new XmlCourseLoader(loadContext));
        loadContext.setSectionLoader(new XmlSectionLoader(loadContext));
        loadContext.setPeriodLoader(new XmlPeriodLoader());
        loadContext.setNotesLoader(new XmlNotesLoader());
        return context;
    }
}
