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

package edu.rpi.scheduler.ui.print;

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.UniqueSection;
import edu.rpi.scheduler.schedb.spec.Schedule;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.widgets.DetailedScheduleGrid;
import edu.rpi.scheduler.ui.widgets.ScheduleGrid;
import edu.rpi.scheduler.ui.widgets.TimeGridSchedulePainter;

import javax.print.event.PrintServiceAttributeEvent;
import javax.print.event.PrintServiceAttributeListener;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Collection;
import java.util.Map;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import static edu.rpi.scheduler.ui.StringFormatType.COURSE_VERY_SHORT;


public class SchedulePrinter {
    //TOSMALL: print course names in grid
    private SchedulingSession session;
    private Schedule schedule;
    private static final boolean PRINT_TO_FILE = false;
    private ScheduleGrid originalGrid = null;

    public SchedulePrinter(SchedulingSession session, Schedule schedule) {
        this.schedule = schedule;
        this.session = session;
    }

    public void print() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Schedule");
        if (!job.printDialog()) return;
        if (PRINT_TO_FILE) {
            try {
                StreamPrintServiceFactory[] factories
                        = PrinterJob.lookupStreamPrintServices("application/postscript");
                final FileOutputStream stream = new FileOutputStream("d:/in/test.ps");
                final StreamPrintService service = factories[0].getPrintService(stream);
                service.addPrintServiceAttributeListener(new PrintServiceAttributeListener() {
                    public void attributeUpdate(PrintServiceAttributeEvent psae) {
                        if (service.isDisposed()) {
                            try {
                                System.out.println("closing");
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                job.setPrintService(service);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
        job.setPrintable(new SchedulePrintable());
        job.print();
    }

    public void setOriginalGrid(ScheduleGrid grid) {
        originalGrid = grid;
    }

    private class SchedulePrintable implements Printable {
        private static final int NUM_LINES = 3;
        private DetailedScheduleGrid grid;
        {
            grid = new DetailedScheduleGrid(session);
            if (originalGrid != null) {
                TimeGridSchedulePainter origPainter = originalGrid.getSchedulePainter();
                Map<UniqueSection,Paint> paints = origPainter.getPaints();
                grid.getSchedulePainter().setPaints(paints);
            }
            grid.setSchedule(schedule);
        }

        public synchronized int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            if (pageIndex != 0) return NO_SUCH_PAGE;

            double x = pageFormat.getImageableX();
            double y = pageFormat.getImageableY();
            final double width = pageFormat.getImageableWidth();
            final double height = pageFormat.getImageableHeight();

            Graphics2D g = (Graphics2D) graphics;
            AffineTransform ot = g.getTransform();
            g.translate(x, y);

            double sy = height * 0.75;
            double maxheight = height * 0.25;
            Collection<UniqueSection> sections = schedule.getSections();
            double blockHeight = maxheight / sections.size();
            double lineHeight = (blockHeight / NUM_LINES) * 0.8;
            System.out.println("line height: " + lineHeight);
            if (lineHeight > 16) {
                lineHeight = 16;
                blockHeight = (lineHeight / 0.8) * NUM_LINES;
            }
            double infoHeight = blockHeight * sections.size();
            double infoy = height - infoHeight;
            double fontsize = lineHeight * 0.8;
            SchedulerUIPlugin plugin = session.getUIPlugin();
            g.setFont(UITools.getLabelFont().deriveFont((float) fontsize));
            int secno = 0;
            for (UniqueSection section : sections) {
                CourseDescriptor cd = section.getCourseDescriptor();
                String str = plugin.getCourseString(cd, COURSE_VERY_SHORT);
                int lineno = 0;
                g.setPaint(grid.getSchedulePainter().getPaint(section));
                int ovalsize = (int) fontsize*11/16;
                int ovaly = (int) (infoy + (blockHeight * secno)
                        + (lineHeight - fontsize)/2 + ((fontsize - ovalsize)/2));
                g.fillOval(1, ovaly, ovalsize, ovalsize);
                g.setColor(Color.BLACK);
                g.drawOval(1, ovaly, ovalsize, ovalsize);
                g.drawString(str, (float) (0+fontsize),
                        (float) (infoy + (blockHeight * secno)
                        + (lineHeight * lineno) + fontsize));
                lineno++;
                g.drawString(cd.getActualCourse().getName(), (float) (0+fontsize),
                        (float) (infoy + (blockHeight * secno)
                        + (lineHeight * lineno) + fontsize));
                lineno++;
                Collection<SectionDescriptor> sds = section.getSectionDescriptors();
                g.drawString("Section" + (sds.size() == 1 ? "" : "s") + " "
                        + UITools.getSectionNumberList(sds), (float) (0+fontsize),
                        (float) (infoy + (blockHeight * secno)
                        + (lineHeight * lineno) + fontsize));
                secno++;
            }

            grid.setSize((int) width, (int) (height - infoHeight));
            grid.print(g);
            g.setTransform(ot);
            return PAGE_EXISTS;
        }
    }
}
