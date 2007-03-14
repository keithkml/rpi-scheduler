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

package edu.rpi.scheduler.ui.panels.narrow;

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.WeekMask;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.panels.DefaultSubpanel;
import edu.rpi.scheduler.ui.widgets.TimeSelector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NarrowDownPanel extends DefaultSubpanel {
    //TOMAYBE: show times for blocked courses in blocked time grid
    //TOMAYBE: allow to see course times from narrow down panel
    //TOMAYBE: show warning icon when user blocked out all the courses, etc.
    //TODO: make sure uncheck section works
    //TODO: fix time selection not updating blocked courses
    //TODO: fix not being able to select a column
    
    private JTree sectionTree;
    private JButton clearButton;
    private TimeSelector timeSelector;
    private JPanel mainPanel;

    private boolean inited = false;

    private List<SectionDescriptor> reqsections = Collections.emptyList();
    private List<SectionDescriptor> optsections = Collections.emptyList();
    private boolean allCoursesAvailable = false;
    private SectionTreeModel treeModel = null;

    private ClearGridAction clearAction;

    {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    public String getNextButtonText() {
        return "Narrow Down";
    }

    public String getPanelTitle() {
        return "Choose times when you don't want class";
    }

    public String getPrevButtonText() {
        return "Narrow Down";
    }

    public String getInfoBarText() {
        if (canProgress()) {
            return null;
        } else {
            Set<SectionDescriptor> goodSections = new HashSet<SectionDescriptor>(
                    treeModel.getGoodSections());
            Set<SectionDescriptor> blocked = treeModel.getBlockedSections();
            goodSections.removeAll(blocked);
            String extra;
            if (goodSections.isEmpty()) {
                if (getEngine().getSelectedCourses().size() == 1) {
                    extra = "the course you selected.";
                } else {
                    extra = "all of the courses you selected.";
                }
            } else {
                Set<Course> goodCourses = new LinkedHashSet<Course>();
                for (SectionDescriptor sd : goodSections) {
                    goodCourses.add(sd.getCourse());
                }
                // this will keep the order of the courses consistent with the
                // way they're listed everywhere else
                List<String> strings = new ArrayList<String>(goodCourses.size());
                for (CourseDescriptor cd : getEngine().getRequiredCourses()) {
                    if (!goodCourses.contains(cd.getActualCourse())) {
                        strings.add(cd.getActualCourse().getName());
                    }
                }
                extra = UITools.listify(strings) + ".";
            }

            if (timeSelector.getTimeMask().isEmpty()) {
                return "You have blocked out all of the sections for " + extra
                        + " Try blocking fewer sections.";

            } else if (blocked.isEmpty()) {
                return "You have blocked out all of the time for " + extra
                        + " Try blocking less time.";

            } else {
                return "You have blocked out all of the time and sections for "
                        + extra + " Try blocking less time or fewer sections.";
            }
        }
    }

    public synchronized void entering() {
        SchedulerEngine scheduler = getEngine();

        // initialize the time selector grid with the official blocked time of
        // this scheduling session
        timeSelector.setSession(getSession());
        timeSelector.setTimeMask(scheduler.getBlockedTime());

        // store the courses chosen to make schedules with
        List<CourseDescriptor> required = scheduler.getRequiredCourses();
        List<CourseDescriptor> extra = scheduler.getExtraCourses();
//        List<CourseDescriptor> all = scheduler.getAllSelectedCourses();

        // create a list of all of the sections in those courses
        List<SectionDescriptor> requiredSections
                = new ArrayList<SectionDescriptor>(required.size()*10);
        List<SectionDescriptor> extraSections
                = new ArrayList<SectionDescriptor>(extra.size()*10);

        catSections(required, requiredSections);
        catSections(extra, extraSections);

        this.reqsections = requiredSections;
        this.optsections = extraSections;

        // initialize the tree model with the selected courses
        treeModel = new SectionTreeModel(required, extra);
        treeModel.setBlockedSections(scheduler.getBlockedSections());
        treeModel.addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
                sectionTree.stopEditing();
                checkSections();
            }

            public void treeNodesInserted(TreeModelEvent e) {
                sectionTree.stopEditing();
            }
            public void treeNodesRemoved(TreeModelEvent e) {
                sectionTree.stopEditing();
            }
            public void treeStructureChanged(TreeModelEvent e) {
                sectionTree.stopEditing();
            }
        });
        sectionTree.setModel(treeModel);

        // initialize the tree with the sections we have
        checkSections();

        // make the section nodes visible in the tree
        Object root = treeModel.getRoot();
        for (int i = 0; i < treeModel.getChildCount(root); i++) {
            Object course = treeModel.getChild(root, i);
            sectionTree.makeVisible(new TreePath(new Object[] {
                root, course, treeModel.getChild(course, 0) }));
        }
    }
    private static void catSections(List<CourseDescriptor> required,
            List<SectionDescriptor> reqsections) {
        for (CourseDescriptor course : required) {
            Collection<Section> sections = course.getActualCourse().getSections();
            List<SectionDescriptor> descs = SectionDescriptor.getDescriptors(
                    course, sections);
            reqsections.addAll(descs);
        }
    }

    public synchronized boolean canProgress() {
        return allCoursesAvailable;
    }

    public synchronized void progressing() {
        SchedulerEngine engine = getEngine();
        engine.setBlockedTime(timeSelector.getTimeMask());
        engine.setBlockedSections(treeModel.getBlockedSections());
    }

    public synchronized void init() {
        if (inited) return;
        inited = true;

        // disable selection
        sectionTree.setSelectionModel(null);

        // set up the checkbox thingy
        SectionTreeCellManager editor = new SectionTreeCellManager(sectionTree);
        sectionTree.setCellRenderer(editor);
        sectionTree.setCellEditor(editor);

        // don't let the user hide sections by double-clicking a department name
        sectionTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) { }

            public void treeWillCollapse(TreeExpansionEvent event)
                    throws ExpandVetoException {
                // no!
                throw new ExpandVetoException(event);
            }
        });

        // when the user releases the mouse, let's check our available
        // sections
        timeSelector.addTimeMaskChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (SwingUtilities.isEventDispatchThread()) {
                    checkSections();
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            checkSections();
                        }
                    });
                }
            }
        });
        clearAction = new ClearGridAction();
        clearButton.setAction(clearAction);

/*
        Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        whatsThisGridLabel.setCursor(hand);
        whatsThisTreeLabel.setCursor(hand);
        whatsThisGridLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(NarrowDownPanel.this,
                        "This screen lets you choose what times\n" +
                        "during the week you want to have free of\n" +
                        "classes - that is, the scheduler will not\n" +
                        "generate schedules that have classes\n" +
                        "during the times you select.\n\nSo, " +
                        "essentially, you are highlighting the\n" +
                        "time you want off from class.",
                        "Time selector help",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        whatsThisTreeLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(NarrowDownPanel.this,
                        "On this screen you can select both which\n" +
                        "times you don't want class and which\n" +
                        "sections you would like to avoid. If, for\n" +
                        "example, you do not like a particular\n" +
                        "instructor, you can uncheck the sections\n" +
                        "of the classes he or she teaches.\n\nOnly " +
                        "checked sections will be included in your\n" +
                        "final generated schedules.",
                        "Time selector help",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
*/
    }

    private synchronized void checkSections() {
        List<SectionDescriptor> reqsects = reqsections;
        List<SectionDescriptor> optsects = optsections;
        SectionTreeModel tree = treeModel;
        SchedulerEngine engine = getEngine();
        TimeSelector timeSelector = this.timeSelector;
        if (engine == null || timeSelector == null || tree == null) return;

        // get the selected required courses from the scheduler
        List<CourseDescriptor> required = engine.getRequiredCourses();

        // and the sections manually unchecked
        Set blockedSections = tree.getBlockedSections();
        WeekMask<?> mask = timeSelector.getTimeMask();
        Set<SectionDescriptor> goodSections
                = new HashSet<SectionDescriptor>(reqsects.size() + optsects.size());

        // go through all of the required sections in these courses
        processSection(reqsects, mask, goodSections);

        // figure out which required courses are available
        Set<Course> goodReqCourses = new HashSet<Course>(required.size());
        for (SectionDescriptor section : goodSections) {
            // if the section fits into the mask and it's not unchecked,
            // add its course to the good course list (for use in
            // enabling/disabling the Next button)
            if (!blockedSections.contains(section)) {
                goodReqCourses.add(section.getCourse());
            }
        }

        // and then the optional sections
        processSection(optsects, mask, goodSections);

        // and tell the tree model about them
        tree.setGoodSections(goodSections);

        // and then tell the scheduler panel to update the next button
//        boolean old = allCoursesAvailable;
        allCoursesAvailable = !goodSections.isEmpty()
                && goodReqCourses.size() == required.size();
        getSchedulerPanel().updateNextStatus();
    }

    private static void processSection(List<SectionDescriptor> sections, WeekMask<?> mask,
            Set<SectionDescriptor> goodSections) {
        for (SectionDescriptor section : sections) {
            // if the section fits into the time mask, add it to the list of
            // good (time) sections
            if (section.getActualSection().getWeekMask().fitsInto(mask)) {
                goodSections.add(section);
            }
        }
    }

    private class ClearGridAction extends AbstractAction {
        public ClearGridAction() {
            super("Clear");
            putValue(MNEMONIC_KEY, KeyEvent.VK_C);

            timeSelector.addTimeMaskChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateEnabled();
                }
            });
            updateEnabled();
        }

        private void updateEnabled() {
            WeekMask<?> mask = timeSelector.getTimeMask();
            setEnabled(mask != null && !mask.isEmpty());
        }

        public void actionPerformed(ActionEvent e) {
            timeSelector.clearTimeMask();
        }
    }
}
