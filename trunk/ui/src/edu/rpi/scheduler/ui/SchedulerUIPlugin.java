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

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.spec.DailyTimePeriod;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchKey;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchType;
import edu.rpi.scheduler.ui.panels.courses.SelectedCoursesList;
import edu.rpi.scheduler.ui.panels.courses.ConflictDetector;

import java.awt.Dimension;
import java.awt.Image;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.List;

public interface SchedulerUIPlugin {
    void loadSettings() throws PluginLoadingException;

    String getProgramName();

    String getAdminName();

    String getAdminEmail();

    String getCourseString(CourseDescriptor cd, StringFormatType type);

    Map<SearchType,Set<SearchKey>> getCourseSearchTypes();

    DailyTimePeriod getPreferredTimeGridRange(TimeGridType type);

    Image getWindowIcon(WindowType type);

    Dimension getDefaultWindowSize(WindowType type);

    String getAboutBoxText();

    Collection<Link> getLinks();

    String getInfoText(CourseDescriptor cd, SelectedCoursesList selectedCourses,
            ConflictDetector conflictDetector);

    List<RankingMethod> getRankingMethods();
}
