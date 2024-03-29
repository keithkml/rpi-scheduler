package edu.rpi.scheduler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NewRpiParser implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(NewRpiParser.class.getName());

    private static final Map<String, String> departmentNames = new HashMap<String, String>();

    static {
        departmentNames.put("ARCH", "Architecture");
        departmentNames.put("ADMN", "Administration");
        departmentNames.put("LGHT", "Lighting");
        departmentNames.put("BMED", "Biomedical Engineering");
        departmentNames.put("CHME", "Chemical Engineering");
        departmentNames.put("CIVL", "Civil Engineering");
        departmentNames.put("ECSE", "Electrical, Computer, and Systems Engineering");
        departmentNames.put("ENGR", "General Engineering");
        departmentNames.put("ENVE", "Environmental Engineering");
        departmentNames.put("EPOW", "EPOW");
        departmentNames.put("ESCI", "Engineering Science");
        departmentNames.put("ISYE", "Industrial and Systems Engineering");
        departmentNames.put("MANE", "Mechanical, Aerospace, and Nuclear Engineering");
        departmentNames.put("MTLE", "Materials Science and Engineering");
        departmentNames.put("ARTS", "Arts");
        departmentNames.put("COMM", "Communication");
        departmentNames.put("IHSS", "Interdisciplinary Humanities and Social Sciences");
        departmentNames.put("LANG", "Foreign Languages and Literature");
        departmentNames.put("LITR", "Literature");
        departmentNames.put("PHIL", "Philosophy");
        departmentNames.put("STSH", "Science and Technology Studies (Humanities Courses)");
        departmentNames.put("WRIT", "Writing");
        departmentNames.put("COGS", "Cognitive Science");
        departmentNames.put("ECON", "Economics");
        departmentNames.put("IHSS", "Interdisciplinary Humanities and Social Science");
        departmentNames.put("PSYC", "Psychology");
        departmentNames.put("STSS", "Science and Technology Studies (Social Sciences Courses)");
        departmentNames.put("ITWS", "Information Technology and Web Science");
        departmentNames.put("MGMT", "Management");
        departmentNames.put("ASTR", "Astronomy");
        departmentNames.put("BCBP", "Biochemistry and Biophysics");
        departmentNames.put("BIOL", "Biology");
        departmentNames.put("CHEM", "Chemistry");
        departmentNames.put("CISH", "Computer Science at Hartford");
        departmentNames.put("CSCI", "Computer Science");
        departmentNames.put("ISCI", "Interdisciplinary Science");
        departmentNames.put("ERTH", "Earth and Environmental Science");
        departmentNames.put("MATH", "Mathematics");
        departmentNames.put("MATP", "Mathematical Programming, Probability, and Statistics");
        departmentNames.put("PHYS", "Physics");
        departmentNames.put("IENV", "Interdisciplinary Environmental Courses");
        departmentNames.put("USAF", "Aerospace Studies (Air Force ROTC)");
        departmentNames.put("USAR", "Military Science (Army ROTC)");
        departmentNames.put("USNA", "Naval Science (Navy ROTC)");
        departmentNames.put("NSST", "Natural Science for School Teachers");
    }

    public byte[] generateCourseXml(InputStream is)
            throws IOException, SAXException, ParserConfigurationException {
        Document doc = parse(is);
        doc.getDocumentElement().normalize();

        // Time Stamp Conversion
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd hh:mm:ss z yyyy");
        String strDateStamp = sdf.format(new Date());

        // XML output begins
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(byteArrayOutputStream, true, "UTF-8");
        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        output.println("<schedb generated=\"" + strDateStamp + "\" minutes-per-block=\"30\">");

        String strDept = "";

        //  Course
        NodeList nodeLstCourse = doc.getElementsByTagName("COURSE");
        for (int s = 0; s < nodeLstCourse.getLength(); s++) {
            Node nodeCourse = nodeLstCourse.item(s);
            if (nodeCourse.getNodeType() == Node.ELEMENT_NODE) {
                Element elmntCourse = (Element) nodeCourse;
                strDept = parseCourse(output, strDept, elmntCourse);

            }
        }
        output.println("</dept>");
        output.println("</schedb>");
        return byteArrayOutputStream.toByteArray();
    }

    private String parseCourse(PrintStream output, String strDept, Element elmntCourse) {
        if ((strDept.length() == 0) || !strDept.equalsIgnoreCase(elmntCourse.getAttribute("dept"))) {
            if (strDept.length() == 0) {
            } else {
                output.println("</dept>");
            }
            output.println("<dept abbrev=\"" + elmntCourse.getAttribute("dept") + "\""
                           + " name=\"" + getDepartmentName(elmntCourse.getAttribute("dept")) + "\"" + ">");
            strDept = elmntCourse.getAttribute("dept");
        }
        String strName = elmntCourse.getAttribute("name");
        if (strName != null && strName.length() > 0) {
            strName = strName.replace("&", "&amp;");
        }

        //  Section
        StringBuilder sectionsXml = new StringBuilder();
        NodeList nodeLstSection = elmntCourse.getElementsByTagName("SECTION");
        for (int s2 = 0; s2 < nodeLstSection.getLength(); s2++) {
            Node nodeSection = nodeLstSection.item(s2);
            if (nodeSection.getNodeType() == Node.ELEMENT_NODE) {
                Element elmntSection = (Element) nodeSection;

                sectionsXml.append(parseSection(elmntSection));
            }
        }

        if (sectionsXml.length() > 0) {
            output.println("<course number=\"" + elmntCourse.getAttribute("num") + "\""
                         + " name=\"" + strName + "\""
                         + " min-credits=\"" + elmntCourse.getAttribute("credmin") + "\""
                         + " max-credits=\"" + elmntCourse.getAttribute("credmax") + "\""
                         + " grade-type=\"" + getGradeTypeString(elmntCourse) + "\"" + ">");
            output.print(sectionsXml);
            output.println("</course>");
        }
        return strDept;
    }

    private String getDepartmentName(String deptAbbrev) {
        String name = departmentNames.get(deptAbbrev);
        if (name != null) return name;
        return deptAbbrev;
    }

    private String getGradeTypeString(Element elmntCourse) {
        String gradetype = elmntCourse.getAttribute("gradetype");
        String realGT = "normal";
        if (gradetype.equals("Satisfactory/Unsatisfactory")) realGT = "pass-fail";
        if (gradetype.equals("Non-graded")) realGT = "no-grade";
        return realGT;
    }

    private String parseSection(Element elmntSection) {
        String num = elmntSection.getAttribute("num");
        try {
            Integer.parseInt(num);
        } catch (NumberFormatException e) {
            // skip this one. these are weird.
            return "";
        }

        StringBuilder periodsXml = new StringBuilder();
        NodeList nodeLstPeriod = elmntSection.getElementsByTagName("PERIOD");
        for (int s3 = 0; s3 < nodeLstPeriod.getLength(); s3++) {
            Node nodePeriod = nodeLstPeriod.item(s3);
            if (nodePeriod.getNodeType() == Node.ELEMENT_NODE) {
                Element elmntPeriod = (Element) nodePeriod;
                periodsXml.append(parsePeriod(elmntPeriod));
            }
        }

        if (periodsXml.length() == 0)
            return "";

        return "<section crn=\"" + elmntSection.getAttribute("crn") + "\""
                       + " number=\"" + num + "\""
                       + " seats=\"" + elmntSection.getAttribute("seats") + "\"" + ">" + periodsXml
                       + "</section>\n";
    }

    private String parsePeriod(Element elmntPeriod) {
        // Days
        String strDays = "";
        String strComma = "";
        NodeList nodeLstDay = elmntPeriod.getElementsByTagName("DAY");
        for (int s4 = 0; s4 < nodeLstDay.getLength(); s4++) {
            Element elmntDay = (Element) nodeLstDay.item(s4);
            NodeList nodeListDays = elmntDay.getChildNodes();
            String strVal = nodeListDays.item(0).getNodeValue();
            if (strVal != null && strVal.equalsIgnoreCase("0")) {
                strDays += strComma + "mon";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("1")) {
                strDays += strComma + "tue";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("2")) {
                strDays += strComma + "wed";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("3")) {
                strDays += strComma + "thu";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("4")) {
                strDays += strComma + "fri";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("5")) {
                strDays += strComma + "sat";
                strComma = ",";
            }
            if (strVal != null && strVal.equalsIgnoreCase("6")) {
                strDays += strComma + "sun";
                strComma = ",";
            }
        }
        if (strDays.length() > 0) {
            strDays = " days=\"" + strDays + "\"";
        }

        String strStartTime = elmntPeriod.getAttribute("start");
        if (strStartTime != null && strStartTime.length() == 4) {
            strStartTime = convertTime(strStartTime);
        } else {
            strStartTime = null;
        }

        String strEndTime = elmntPeriod.getAttribute("end");
        if (strEndTime != null && strEndTime.length() == 4) {
            strEndTime = convertTime(strEndTime);
        } else {
            strEndTime = null;
        }

        if (strStartTime == null || strEndTime == null)
            return "";

        String type = getPeriodType(elmntPeriod.getAttribute("type"));
        if (type == null) {
            LOGGER.warning("Unknown period type " + elmntPeriod.getAttribute("type") + " for "
                           + elmntPeriod.getParentNode().getParentNode().getAttributes().getNamedItem("name"));
            type = "lecture";
        }
        return "<period type=\"" + type + "\""
                     + " professor=\"" + elmntPeriod.getAttribute("instructor") + "\" "
                     + strDays + " starts=\"" + strStartTime + "\""
                     + " ends=\"" + strEndTime + "\"/>\n";
    }

    private String getPeriodType(String type) {
        type = type.toUpperCase();
        if (type.equals("LEC")) return "lecture";
        if (type.equals("REC")) return "recitation";
        if (type.equals("LAB")) return "lab";
        if (type.equals("STU")) return "studio";
        if (type.equals("SEM")) return "seminar";
        if (type.equals("TES")) return "lecture"; //TODO: create test period type
        if (type.trim().equals("")) return "lecture"; //TODO: create test period type
        return null;
    }

    private Document parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });
        return db.parse(is);
    }

    private static String convertTime(String s) {
        int intHours = Integer.parseInt(s.substring(0, 2));
        int intMins = Integer.parseInt(s.substring(2, 4));
        // convert times from 24hr to AM/PM
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, intHours);
        cal.set(Calendar.MINUTE, intMins);
        Date date = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mma");
        sdf.setCalendar(cal);
        cal.setTime(date);
        return sdf.format(date);
    }
}