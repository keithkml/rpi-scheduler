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

import edu.rpi.scheduler.schedb.spec.Department;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

class DeptBoxRenderer extends DefaultListCellRenderer {
    private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(2, 2, 2, 2);

    private JPanel panel;
    private JLabel nameLabel;
    private JLabel abbrevLabel;

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
//        Component comp = list;
//        System.out.println("Parents:");
//        while (comp != null) {
//            System.out.println("+ " + comp);
//            if (comp instanceof ComboPopup) {
//                System.out.println("  - resizing, old: " + comp.getSize());
//                comp.setSize(500, 500);
//                System.out.println("    new: " + comp.getSize());
//            }
//            comp = comp.getParent();
//        }
//        System.out.println();
        if (value == null) {
            super.getListCellRendererComponent(list, "All courses",
                    index, isSelected, cellHasFocus);
            setBorder(EMPTY_BORDER);
            return this;
        }

        super.getListCellRendererComponent(list, "", index, isSelected,
                cellHasFocus);
        Department dept = (Department) value;
        panel.setBackground(getBackground());
        cloneLabel(this, nameLabel);
        cloneLabel(this, abbrevLabel);
        nameLabel.setText(getBigName(dept));
        abbrevLabel.setText(getLittleName(dept));
        return panel;
    }

    private void cloneLabel(JLabel source, JLabel dest) {
        dest.setBackground(source.getBackground());
        dest.setForeground(source.getForeground());
    }

    public String getBigName(Department dept) {
        String name = getName(dept);
        String abbrev = getAbbrev(dept);
        return name == null
                ? (abbrev == null ? "(No name available)" : abbrev)
                : name;
    }

    public String getLittleName(Department dept) {
        String abbrev = getAbbrev(dept);
        return abbrev == null ? "????" : abbrev;
    }

    private String getAbbrev(Department dept) {
        String abbrev = dept.getAbbrev();
        return getIfNotEmpty(abbrev);
    }

    private String getName(Department dept) {
        String name = dept.getName();
        return getIfNotEmpty(name);
    }

    private String getIfNotEmpty(String str) {
        if (str != null && str.trim().length() == 0) str = null;
        return str;
    }
}
