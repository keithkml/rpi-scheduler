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

import edu.rpi.scheduler.schedb.DefaultSchedulerData;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoadListener;
import edu.rpi.scheduler.schedb.load.spec.DatabaseLoader;
import edu.rpi.scheduler.schedb.load.spec.DepartmentLoader;
import edu.rpi.scheduler.schedb.spec.DataContext;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.ResourceLoader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.SECTION;
import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.COURSE;
import static edu.rpi.scheduler.schedb.load.spec.DataObjectType.DEPARTMENT;
import static java.text.DateFormat.SHORT;

public class XmlDatabaseLoader extends XmlLoader implements DatabaseLoader {
    private static final Logger logger
            = Logger.getLogger(XmlDatabaseLoader.class.getName());

    private DataContext context;
    private MutableDataLoadingContext dataContext;
    private Document document;
    private ResourceLoader resourceLoader = null;

    public XmlDatabaseLoader(DataContext context) {
        super((MutableDataLoadingContext) context.getDataLoadingContext());
        MutableDataLoadingContext dataContext
                = (MutableDataLoadingContext) context.getDataLoadingContext();

        this.context = context;
        this.dataContext = dataContext;
        dataContext.setSchedulerDataObj(new DefaultSchedulerData(context));
    }

    public void setResourceLoader(ResourceLoader loader) {
        this.resourceLoader = loader;
    }

    public void loadDb(String uri, DatabaseLoadListener listener)
            throws DbLoadException {
        dataContext.setDatabaseLoadListener(listener);
        try {
            loadData(uri);
        } catch (DbLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new DbLoadException("Couldn't load " + uri, e);
        } finally {
            // free memory
            document = null;
        }
    }

    private void loadData(String uri) throws IOException,
            ParserConfigurationException, SAXException, DbLoadException {
        Document doc = loadDocument(uri);
        if (doc == null) {
            throw new DbLoadException("Unknown error while loading database");
        }
        parseDocument(doc);
    }

    protected Document loadDocument(String url) throws IOException,
            ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        document = builder.parse(resourceLoader.loadResource(url));
        return document;
    }

    protected void parseDocument(Document doc) throws DbLoadException {
        Element root = doc.getDocumentElement();
        if (!root.getNodeName().equals("schedb")) {
            throw new DbLoadException("root element is '" + root.getNodeName()
                    + "', but it should be 'schedb'");
        }

        DefaultSchedulerData dataobj
                = (DefaultSchedulerData) dataContext.getSchedulerDataObj();
        Attr idNode = root.getAttributeNode("data-set-id");
        if (idNode != null) dataobj.setDataSetId(idNode.getValue());
        Attr modNode = root.getAttributeNode("last-modified");
        if (modNode != null) {
            String datestr = modNode.getValue();
            DateFormat timeReader = getStandardDateFormat();
            try {
                dataobj.setLastModified(timeReader.parse(datestr));
            } catch (ParseException ignored) { }
        }

        parseDepts(root);
        parseCustomData(root);
    }

    protected void parseCustomData(Element root) {
    }

    private static DateFormat getStandardDateFormat() {
        return DateFormat.getDateTimeInstance(SHORT, SHORT, Locale.US);
    }

    /*
    <schedb generated="Wed Nov 05 02:07:08 EST 2003">
    <dept name="ADMN">
    <course number="1010" name="ORAL COMM FOR TA'S I" seats="15" min-credits="0"
            max-credits="0" grade-type="pass-fail">
    <section crn="90588" number="1">
    <period type="lecture" professor="Steigler" days="mon,thu" starts="8:00AM"
            ends="9:20AM" />

    */
    protected void parseDepts(Element root) {
        DefaultSchedulerData dataobj
                = (DefaultSchedulerData) dataContext.getSchedulerDataObj();

        NodeList deptels = root.getElementsByTagName("dept");
        int courses = 0;
        int sections = 0;
        for (int i = 0; i < deptels.getLength(); i++) {
            Element deptel = (Element) deptels.item(i);
            NodeList children = deptel.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (node.getNodeName().equals("course")) {
                    courses++;
                    NodeList seclist = node.getChildNodes();
                    for (int k = 0; k < seclist.getLength(); k++) {
                        Node secnode = seclist.item(k);
                        if (secnode.getNodeName().equals("section")) {
                            sections++;
                        }
                    }
                }
            }
        }
        dataContext.increaseTotalObjectCount(DEPARTMENT, deptels.getLength());
        dataContext.increaseTotalObjectCount(COURSE, courses);
        dataContext.increaseTotalObjectCount(SECTION, sections);
        for (int i = 0; i < deptels.getLength(); i++) {
            Element deptel = (Element) deptels.item(i);
            try {
                dataobj.addCompletedDepartment(parseDept(deptel));
            } catch (DbLoadException e) {
                logger.log(Level.WARNING, "Error parsing department entry", e);
                continue;
            }
        }
    }

    protected Department parseDept(Element deptel) throws DbLoadException {
        DepartmentLoader deptLoader = dataContext.getDeptLoader();
        return deptLoader.loadDepartment(new XmlDataElement(deptel));
    }
}
