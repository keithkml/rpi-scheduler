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

package edu.rpi.scheduler.schedb.load;

import edu.rpi.scheduler.schedb.DefaultDepartment;
import edu.rpi.scheduler.schedb.load.spec.CourseLoader;
import edu.rpi.scheduler.schedb.load.spec.DataElement;
import edu.rpi.scheduler.schedb.load.spec.DepartmentLoader;
import edu.rpi.scheduler.schedb.spec.Course;
import edu.rpi.scheduler.schedb.spec.Department;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.DEPARTMENT;

public class XmlDeptLoader extends XmlLoader implements DepartmentLoader {
    private static final Logger logger
            = Logger.getLogger(XmlDeptLoader.class.getPackage().getName());

    public XmlDeptLoader(MutableDataLoadingContext context) {
        super(context);
    }

    public Department loadDepartment(
            DataElement element) throws DbLoadException {
        XmlDataElement xel = (XmlDataElement) element;

        Element deptel = xel.getDomElement();

        String abbrev = readAbbrev(deptel);
        String name = readName(deptel);
        String use = null;
        if (name != null) use = name;
        else if (abbrev != null) use = abbrev;
        if (use != null) getMutableContext().fireLoadingObject(DEPARTMENT, use);

        DefaultDepartment dept = createDepartment(abbrev, name);

        readCourses(deptel, dept);
        readCustomData(deptel, dept);

        return dept;
    }

    protected void readCustomData(Element deptel, Department dept) {
    }

    protected String readAbbrev(Element deptel) throws DbLoadException {
        return deptel.getAttribute("abbrev");
    }

    protected String readName(Element deptel) throws DbLoadException {
        return deptel.getAttribute("name");
    }

    protected DefaultDepartment createDepartment(String abbrev, String name) {
        return new DefaultDepartment(abbrev, name);
    }

    protected void readCourses(Element deptel, DefaultDepartment dept)
            throws DbLoadException {
        NodeList coursels = deptel.getElementsByTagName("course");
        for (int i = 0; i < coursels.getLength(); i++) {
            Element coursel = (Element) coursels.item(i);
            try {
                dept.addCourse(loadCourse(coursel));
            } catch (DbLoadException e) {
                logger.log(Level.WARNING, "DB format exception while loading "
                        + "department '" + dept.getName() + "'", e);
                continue;
            }
        }
    }

    protected Course loadCourse(Element coursel)
            throws DbLoadException {
        CourseLoader courseLoader = getContext().getCourseLoader();
        return courseLoader.loadCourse(
                new XmlDataElement(coursel));
    }
}
