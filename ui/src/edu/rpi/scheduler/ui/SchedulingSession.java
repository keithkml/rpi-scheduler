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

package edu.rpi.scheduler.ui;

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.SchedulerDataPlugin;
import edu.rpi.scheduler.schedb.spec.ResourceLoader;

import java.net.URL;

public class SchedulingSession {
    private URL codebase = null;
    private String dbUrl = null;

    private SchedulerEngine engine = new SchedulerEngine();
    private SchedulerDataPlugin dataPlugin = null;
    private SchedulerUIPlugin uiPlugin = null;
    private DataContext dataContext = null;
    private ResourceLoader resourceLoader = new CachingResourceLoader();

    public SchedulerEngine getEngine() {
        return engine;
    }

    public void setEngine(SchedulerEngine engine) {
        this.engine = engine;
    }

    public SchedulerDataPlugin getDataPlugin() {
        return dataPlugin;
    }

    public void setDataPlugin(SchedulerDataPlugin dataPlugin) {
        this.dataPlugin = dataPlugin;
        dataContext = dataPlugin.newSchedulingContext();
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    public void setDatabaseLocation(String url) {
        this.dbUrl = url;
    }

    public String getDatabaseLocation() {
        return dbUrl;
    }

    public URL getCodebase() {
        return codebase;
    }

    public void setCodebase(URL codebase) {
        this.codebase = codebase;
    }

    public void setUIPlugin(SchedulerUIPlugin uiPlugin) {
        this.uiPlugin = uiPlugin;
    }

    public SchedulerUIPlugin getUIPlugin() {
        return uiPlugin;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
