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
import edu.rpi.scheduler.ui.StringFormatType;
import edu.rpi.scheduler.schedb.CourseDescriptor;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Collection;
import java.io.IOException;
import java.io.StringReader;

public class TransferableCourseList implements Transferable {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private SchedulingSession session;
    private Collection<CourseDescriptor> courses;
    private DataFlavor[] flavors;
    private CourseAction action;

    public TransferableCourseList(SchedulingSession session,
            Collection<CourseDescriptor> courses,
            CourseAction action) {
        this.session = session;
        this.courses = courses;
        if (action == CourseAction.ADD) flavors = CourseTransferHandler.ADD_FLAVORS;
        else if (action == CourseAction.REMOVE) flavors = CourseTransferHandler.REMOVE_FLAVORS;
        this.action = action;
    }

    public DataFlavor[] getTransferDataFlavors() { return flavors; }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor oflavor : getTransferDataFlavors()) {
            if (flavor.match(oflavor)) return true;
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor.match(CourseTransferHandler.FLAVOR_ADDED_COURSE)) {
            if (action == CourseAction.ADD) {
                return new DraggedAddedCourseList(courses);
            } else {
                return null;
            }

        } else if (flavor.match(CourseTransferHandler.FLAVOR_REMOVED_COURSE)) {
            if (action == CourseAction.REMOVE) {
                return new DraggedRemovedCourseList(courses);
            } else {
                return null;
            }

        } else if (flavor.match(CourseTransferHandler.FLAVOR_TEXT)) {
            StringBuffer buf = new StringBuffer();
            boolean first = true;
            for (CourseDescriptor cd : courses) {
                if (!first) {
                    buf.append(LINE_SEPARATOR);
                } else {
                    first = false;
                }
                buf.append(session.getUIPlugin().getCourseString(cd, StringFormatType.COURSE_SHORT));
            }
            return new StringReader(buf.toString());

        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
