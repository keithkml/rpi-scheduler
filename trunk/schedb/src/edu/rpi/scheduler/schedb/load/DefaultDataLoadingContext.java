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

import edu.rpi.scheduler.DefensiveTools;
import edu.rpi.scheduler.schedb.load.spec.CourseLoader;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoadListener;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoader;
import edu.rpi.scheduler.schedb.load.spec.DepartmentLoader;
import edu.rpi.scheduler.schedb.load.spec.NotesLoader;
import edu.rpi.scheduler.schedb.load.spec.PeriodLoader;
import edu.rpi.scheduler.schedb.load.spec.SectionLoader;
import edu.rpi.scheduler.schedb.load.spec.DataObjectType;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.SchedulerData;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;

import java.util.HashMap;
import java.util.Map;

public class DefaultDataLoadingContext implements MutableDataLoadingContext {
    private final SchedulerDataPlugin plugin;
    private final DataContext context;

    private SchedulerData schedulerData = null;

    private DatabaseLoader fileLoader = null;
    private DepartmentLoader deptLoader = null;
    private CourseLoader courseLoader = null;
    private SectionLoader sectionLoader = null;
    private PeriodLoader periodLoader = null;
    private NotesLoader notesLoader = null;
    private DatabaseLoadListener listener = null;
    private Map<DataObjectType,Integer> loadedObjectCount
            = new HashMap<DataObjectType, Integer>();
    private Map<DataObjectType,Integer> totalObjectCount
            = new HashMap<DataObjectType, Integer>();

    {
        for (DataObjectType type : DataObjectType.values()) {
            loadedObjectCount.put(type, 0);
            totalObjectCount.put(type, 0);
        }
    }

    public DefaultDataLoadingContext(SchedulerDataPlugin plugin,
            DataContext context) {
        DefensiveTools.checkNull(plugin, "plugin");
        DefensiveTools.checkNull(context, "context");

        this.plugin = plugin;
        this.context = context;
    }

    public SchedulerDataPlugin getPlugin() { return plugin; }

    public DataContext getSchedulingContext() { return context; }

    public DatabaseLoadListener getDataLoadListener() { return listener; }

    public int getLoadedObjectCount(DataObjectType type) {
        Integer count = loadedObjectCount.get(type);
        if (count == null) return 0;
        return count;
    }

    public int getTotalObjectCount(DataObjectType type) {
        Integer count = totalObjectCount.get(type);
        if (count == null) return 0;
        return count;
    }

    public void fireLoadingObject(DataObjectType type,
            String name) {
        DatabaseLoadListener listener = this.listener;
        if (listener == null) return;
        if (type == DataObjectType.DEPARTMENT) {
            listener.loadingDepartment(this, name);
        } else if (type == DataObjectType.COURSE) {
            listener.loadingCourse(this, name);
        } else if (type == DataObjectType.SECTION) {
            listener.loadingSection(this, name);
        }
        incrLoadedCount(type);
    }

    public void increaseTotalObjectCount(DataObjectType type, int count) {
        totalObjectCount.put(type, getTotalObjectCount(type) + count);
    }

    private void incrLoadedCount(DataObjectType type) {
        loadedObjectCount.put(type, getLoadedObjectCount(type) + 1);
    }

    public SchedulerData getSchedulerDataObj() { return schedulerData; }

    public void setDatabaseLoadListener(DatabaseLoadListener listener) {
        this.listener = listener;
    }

    public void setSchedulerDataObj(SchedulerData schedulerData) {
        this.schedulerData = schedulerData;
    }

    public final DatabaseLoader getFileLoader() { return fileLoader; }

    public final void setFileLoader(DatabaseLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    public final DepartmentLoader getDeptLoader() { return deptLoader; }

    public final void setDeptLoader(DepartmentLoader deptLoader) {
        this.deptLoader = deptLoader;
    }

    public final CourseLoader getCourseLoader() { return courseLoader; }

    public final void setCourseLoader(CourseLoader courseLoader) {
        this.courseLoader = courseLoader;
    }

    public final SectionLoader getSectionLoader() { return sectionLoader; }

    public void setSectionLoader(SectionLoader sectionLoader) {
        this.sectionLoader = sectionLoader;
    }

    public final PeriodLoader getPeriodLoader() { return periodLoader; }

    public final void setPeriodLoader(PeriodLoader periodLoader) {
        this.periodLoader = periodLoader;
    }

    public final NotesLoader getNotesLoader() { return notesLoader; }

    public final void setNotesLoader(NotesLoader notesLoader) {
        this.notesLoader = notesLoader;
    }
}
