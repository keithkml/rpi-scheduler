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

package edu.rpi.scheduler.ui.panels.courses;

import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.schedb.CourseDescriptor;

import javax.swing.TransferHandler;
import javax.swing.JComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class CourseTransferHandler extends TransferHandler {
    private static final Logger logger
            = Logger.getLogger(CourseTransferHandler.class.getName());

    public static final DataFlavor FLAVOR_ADDED_COURSE
            = new CustomFlavor(DraggedAddedCourseList.class);
    public static final DataFlavor FLAVOR_REMOVED_COURSE
            = new CustomFlavor(DraggedRemovedCourseList.class);
    public static final DataFlavor FLAVOR_TEXT
            = DataFlavor.getTextPlainUnicodeFlavor();
    public static final DataFlavor[] ADD_FLAVORS = new DataFlavor[] {
        FLAVOR_ADDED_COURSE,
        FLAVOR_TEXT,
    };
    public static final DataFlavor[] REMOVE_FLAVORS = new DataFlavor[] {
        FLAVOR_REMOVED_COURSE,
        FLAVOR_TEXT,
    };

    private DataFlavor desiredImportFlavor;
    private SchedulingSession session;

    public CourseTransferHandler(SchedulingSession session) {
        this.session = session;
        CourseAction action = getCourseImportAction();
        if (action == CourseAction.ADD) {
            desiredImportFlavor = FLAVOR_ADDED_COURSE;
        } else if (action == CourseAction.REMOVE) {
            desiredImportFlavor = FLAVOR_REMOVED_COURSE;
        }
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        for (DataFlavor flavor : transferFlavors) {
            if (flavor.match(desiredImportFlavor)) return true;
        }
        return false;
    }

    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    protected Transferable createTransferable(JComponent c) {
        return new TransferableCourseList(session, getSelectedCourses(),
                getCourseExportAction());
    }

    public boolean importData(JComponent comp, Transferable t) {
        List<CourseDescriptor> data = new ArrayList<CourseDescriptor>();
        try {
            data.addAll(((DraggedCourseList)
                    t.getTransferData(desiredImportFlavor)).getCourses());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't accept course drag", e);
            return false;
        }
        if (data.isEmpty()) return false;
        importCourses(data);
        return true;
    }

    protected abstract Collection<CourseDescriptor> getSelectedCourses();
    protected abstract void importCourses(List<CourseDescriptor> data);
    protected abstract CourseAction getCourseImportAction();
    protected abstract CourseAction getCourseExportAction();
}
