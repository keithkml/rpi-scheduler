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

import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.SectionDescriptor;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Section;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides a tree model representing courses (toplevel nodes) and their
 * sections (leaves).
 */
public class SectionTreeModel implements TreeModel {
    private final Object root = new Object();

    private final List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    private final List<CourseNode> courseNodes;
    private Set<SectionDescriptor> goodSections = Collections.emptySet();
    private Set<SectionDescriptor> blockedList = Collections.emptySet();

    public SectionTreeModel(List<CourseDescriptor> orequired, List<CourseDescriptor> oextra) {
        List<CourseDescriptor> required = new ArrayList<CourseDescriptor>(orequired);
        List<CourseDescriptor> extra = new ArrayList<CourseDescriptor>(oextra);
        Collections.sort(required);
        Collections.sort(extra);

        List<CourseNode> courseNodes = new ArrayList<CourseNode>(required.size() + extra.size());
        for (CourseDescriptor course : required) {
            courseNodes.add(new CourseNode(course, true));
        }
        for (CourseDescriptor course : extra) {
            courseNodes.add(new CourseNode(course, false));
        }
        this.courseNodes = courseNodes;
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        if (parent == root) {
            return courseNodes.get(index);
        } else if (parent instanceof CourseNode) {
            CourseNode cn = (CourseNode) parent;

            return cn.getChild(index);
        } else {
            return null;
        }
    }

    public int getChildCount(Object parent) {
        if (parent == root) {
            return courseNodes.size();
        } else if (parent instanceof CourseNode) {
            CourseNode cn = (CourseNode) parent;
            return cn.getChildCount();
        } else {
            return 0;
        }
    }

    public boolean isLeaf(Object node) {
        return node instanceof SectionNode;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        for (TreeModelListener listener : listeners) {
            listener.treeNodesChanged(new TreeModelEvent(this, path));
        }
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) return -1;

        if (parent == root) {
            return courseNodes.indexOf(child);

        } else if (parent instanceof CourseNode
                && child instanceof SectionNode) {
            CourseNode cn = (CourseNode) parent;

            return cn.getIndexOf((SectionNode) child);
        } else {
            return -1;
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    /**
     * Sets the list of "good sections." Sections of the courses in this model
     * that are not present in {@code goodSections} will be hidden from the
     * user.
     * @param goodSections the list of sections to be shown
     */
    public void setGoodSections(Set<SectionDescriptor> goodSections) {
        this.goodSections = goodSections;

        for (CourseNode node : courseNodes) {
            node.updateGoodSections();
        }
    }

    public Set<SectionDescriptor> getGoodSections() {
        return new HashSet<SectionDescriptor>(goodSections);
    }

    /**
     * Sets the list of "blocked sections." Sections of courses in this model
     * that are present in the given list will be unchecked.
     * @param blocked the list of sections to hide
     */
    public void setBlockedSections(Collection<SectionDescriptor> blocked) {
        this.blockedList = new HashSet<SectionDescriptor>(blocked);

        for (CourseNode node : courseNodes) {
            node.updateBlockedSections();
        }
    }

    public Set<SectionDescriptor> getBlockedSections() {
        return blockedList;
    }

    public void pathChanged(TreePath path) {
        valueForPathChanged(path, path.getLastPathComponent());
    }

    public class CourseNode {
        private final CourseDescriptor course;
        private final boolean required;
        private final List<SectionDescriptor> sections;
        private final List<SectionNode> nodes;
        private final List<SectionNode> visible;
        private final SectionNode noneNode = new SectionNode(this, null);
        private final String desc;

        public CourseNode(CourseDescriptor course, boolean required) {
            this.course = course;
            this.required = required;

            List<SectionDescriptor> sects = SectionDescriptor.getDescriptors(
                    course, course.getActualCourse().getSections());
            Collections.sort(sects);
            this.sections = sects;

            List<SectionNode> nodes = new ArrayList<SectionNode>(sects.size());
            for (SectionDescriptor section : sects) {
                nodes.add(new SectionNode(this, section));
            }
            this.nodes = nodes;

            visible = new ArrayList<SectionNode>(nodes);

            desc = "<HTML><B>" + course.getActualCourse().getName();
        }

        private Object[] getPath() {
            return new Object[] { root, this };
        }


        private int findPos(int end, Section course) {
            int index = Math.min(visible.size(), end);
            for (ListIterator<SectionNode> it = visible.listIterator(index);
                 it.hasPrevious();) {
                SectionNode c = it.previous();

                for (int i = end - 1; i >= 0; i--) {
                    if (sections.get(i).equals(c.getSectionDescriptor())) {
                        return it.previousIndex() + 2;
                    }
                }
            }

            return 0;
        }

        public void updateGoodSections() {
            Set<SectionDescriptor> goodsects = goodSections;
            List<SectionDescriptor> sects = sections;
            List<SectionNode> nodes = this.nodes;
            List<SectionNode> visible = this.visible;
            for (int i = 0; i < sects.size(); i++) {
                SectionDescriptor section = sects.get(i);
                SectionNode node = nodes.get(i);
                if (goodsects.contains(section)) {
                    if (visible.contains(node)) {
                        if (!node.isValid()) {
                            node.setValid(true);
                            int index = visible.indexOf(node);
                            if (index != -1) fireNodeChanged(index, node);
                        }
                    } else {
                        int pos = findPos(i, section.getActualSection());
                        visible.add(pos, node);
                        fireNodeAdded(pos, node);
                    }
                } else {
                    int index = visible.indexOf(node);
                    if (node.isValid()) {
                        node.setValid(false);
                        if (index != -1) {
                            fireNodeChanged(index, node);
                        }
                    }
//                    if (index != -1) {
//                        visible.remove(index);
//                        fireNodeRemoved(index, node);
//                    }
                }
            }

            SectionNode none = noneNode;
            int index = visible.indexOf(none);
            if (visible.size() == 0) {
                // fun fun
                visible.add(none);
                fireNodeAdded(0, none);
            } else if (index != -1 && visible.size() > 1) {
                visible.remove(index);
                fireNodeRemoved(index, none);
            }
        }

        private void fireNodeChanged(int index, SectionNode node) {
            for (TreeModelListener listener : listeners) {
                listener.treeNodesChanged(new TreeModelEvent(this,
                        getPath(), new int[]{index},
                        new Object[]{node}));
            }
        }

        private void fireNodeAdded(int pos, SectionNode node) {
            for (TreeModelListener listener : listeners) {
                listener.treeNodesInserted(new TreeModelEvent(this,
                        getPath(), new int[]{pos},
                        new Object[]{node}));
            }
        }

        private void fireNodeRemoved(int index, SectionNode node) {
            for (Iterator<TreeModelListener> it = listeners.iterator(); it.hasNext();) {
                TreeModelListener listener
                        = it.next();
                listener.treeNodesRemoved(new TreeModelEvent(this,
                        getPath(), new int[] { index },
                        new Object[] { node }));
            }
        }

        public CourseDescriptor getCourse() {
            return course;
        }

        public boolean isRequired() { return required; }

        public String toString() { return desc; }

        public int getChildCount() {
            return visible.size();
        }

        public SectionNode getChild(int index) {
            return visible.get(index);
        }

        public int getIndexOf(SectionNode node) {
            return visible.indexOf(node);
        }

        public void updateBlockedSections() {
            for (SectionNode node : nodes) {
              node.updateBlocked();
            }
        }
    }

    public class SectionNode {
        private final TreePath path;
        private final CourseNode parent;
        private final SectionDescriptor section;
        private boolean checked;
        private String tostring = null;
        private boolean valid = true;

        public SectionNode(CourseNode parent, SectionDescriptor section) {
            this.parent = parent;
            this.section = section;
            this.path = new TreePath(new Object[] { root, parent, this });
            checked = (section != null);
        }

        public SectionDescriptor getSectionDescriptor() {
            return section;
        }

        public Section getSection() {
            return section == null ? null : section.getActualSection();
        }

        public String toString() {
            if (tostring == null) {
                Section actualSection = section.getActualSection();
                Collection<ClassPeriod> periods = actualSection.getPeriods();
                SortedSet<String> profset = new TreeSet<String>();
                StringBuffer tostring = new StringBuffer(20);
                tostring.append(actualSection.getNumber());
                boolean first = true;
                for (ClassPeriod period : periods) {
                    profset.add(period.getProfessor().getName());
                }
                if (!profset.isEmpty()) {
                    tostring.append(" - ");
                    for (String name : profset) {
                        if (!first) tostring.append(", ");
                        else first = false;

                        tostring.append(name);
                    }
                }
                this.tostring = tostring.toString();
            }
            return tostring;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;

            if (this.checked) blockedList.remove(section);
            else blockedList.add(section);

            valueForPathChanged(getPath(), this);
        }

        public boolean isChecked() {
            return checked;
        }

        public void updateBlocked() {
            checked = section == null ? false : !blockedList.contains(section);
        }

        private TreePath getPath() {
            return path;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() { return valid; }

        public CourseNode getCourseNode() { return parent; }
    }
}
