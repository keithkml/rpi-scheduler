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
import edu.rpi.scheduler.schedb.spec.SchedulerData;

import javax.swing.DefaultComboBoxModel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Provides a combo box model containing a list of departments. It's pretty
 * basic.
 */
public class DepartmentCBModel extends DefaultComboBoxModel {
    /**
     * Creates a model containing the departments currently inside the given
     * scheduler.
     * @param sched the scheduler from which to display departments
     */
    public DepartmentCBModel(SchedulerData sched) {
        super(getSortedDepts(sched));
    }

    private static Department[] getSortedDepts(SchedulerData sched) {
        Collection<Department> deptList = sched.getDepartments();
        Department[] departments = deptList.toArray(new Department[deptList.size()]);
        Arrays.sort(departments, new DeptComparator());

        Department[] actual = new Department[departments.length + 1];
        System.arraycopy(departments, 0, actual, 1, departments.length);

        return actual;
    }

    /**
     * Returns the department currently selected by the user.
     * @return the currently selected department
     */
    public Department getSelectedDept() {
        return (Department) getSelectedItem();
    }

    public void setSelectedDept(String abbr) {
        for (int i = 0; i < getSize(); i++) {
            Department el = (Department) getElementAt(i);
            if (el != null && el.getAbbrev().equals(abbr)) {
                setSelectedItem(el);
                break;
            }
        }
    }

    private static class DeptComparator implements Comparator<Department> {
        public int compare(Department o1, Department o2) {
            return o1.getAbbrev().compareTo(o2.getAbbrev());
        }
    }
}
