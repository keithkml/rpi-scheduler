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

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * Provides a means of rendering and "editing" nodes in a section selection
 * tree. Editing means checking or unchecking.
 */
public class SectionTreeCellManager
        implements TreeCellEditor, TreeCellRenderer {
    private final DefaultTreeCellRenderer courseRenderer = new DefaultTreeCellRenderer();
    private final DefaultTreeCellEditor courseEditor;
    private final JCheckBox sectionRenderer = new JCheckBox() {
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            super.paintComponent(g);
        }
    };
    private final JCheckBox editor = new JCheckBox() {
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            super.paintComponent(g);
        }
    };
    private Object currentValue = null;

    private List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    {
        editor.setBorderPaintedFlat(true);
        editor.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (isSection(currentValue)) {
                    SectionTreeModel.SectionNode node
                            = (SectionTreeModel.SectionNode) currentValue;
                    node.setChecked(e.getStateChange()
                            == ItemEvent.SELECTED);
                }
            }
        });
    }

    public SectionTreeCellManager(JTree tree) {
        courseEditor = new DefaultTreeCellEditor(tree, courseRenderer);
    }

    private static void prepareCheckbox(JTree tree, JCheckBox box,
            Object value) {
        SectionTreeModel.SectionNode node
                = (SectionTreeModel.SectionNode) value;
        box.setBackground(tree.getBackground());
        String str = node.toString();
        if (!node.isValid()) {
            str = "<HTML><FONT STYLE=\"text-decoration: line-through\">" + str;
        }
        box.setText(str);
        box.setSelected(node.isValid() && node.isChecked());
        box.setEnabled(node.isValid());
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        if (isSection(value)) {
            JCheckBox renderer = sectionRenderer;
            prepareCheckbox(tree, renderer, value);
            return renderer;
        } else {
            courseRenderer.getTreeCellRendererComponent(tree, value,
                                selected, expanded, leaf, row, hasFocus);
            courseRenderer.setIcon(null);
            return courseRenderer;
        }
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value,
            boolean isSelected, boolean expanded, boolean leaf, int row) {
        currentValue = value;
        if (isSection(value)) {
            prepareCheckbox(tree, editor, value);
            return editor;
        } else {
            return courseEditor.getTreeCellEditorComponent(tree, value,
                    isSelected, expanded, leaf, row);
        }
    }

    private static boolean isSection(Object value) {
        return value instanceof SectionTreeModel.SectionNode
                && ((SectionTreeModel.SectionNode) value).getSection()
                != null;
    }

    public Object getCellEditorValue() {
        return currentValue;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    public boolean stopCellEditing() {
        currentValue = null;
        for (CellEditorListener l : listeners) {
            l.editingStopped(new ChangeEvent(this));
        }
        return true;
    }

    public void cancelCellEditing() {
        currentValue = null;
        for (CellEditorListener l : listeners) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }

    public void addCellEditorListener(CellEditorListener l) {
        listeners.add(l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }
}
