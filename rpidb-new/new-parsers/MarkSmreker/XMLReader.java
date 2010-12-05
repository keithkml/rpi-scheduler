import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class XMLReader {

    public static void main(String argv[]) {

        try {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            //  File file = new File("c:\\scheduler\\201101.xml");
            //  Document doc = db.parse(file);
            Document doc = db.parse(System.in);
            doc.getDocumentElement().normalize();


            // Time Stamp Conversion
            String strTimeStamp = doc.getDocumentElement().getAttribute("timestamp");
            long longTimeStamp = 0;
            String strDateStamp = "";
            try {
                longTimeStamp = Long.parseLong(strTimeStamp.trim());
            } catch (NumberFormatException nfe) {
                //System.out.println("NumberFormatException: " + nfe.getMessage());
            }

            if (longTimeStamp > 0) {
                //System.out.println("timestamp = "+longTimeStamp);
                Date date = new Date(longTimeStamp);
                Calendar cal = new GregorianCalendar();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd hh:mm:ss z yyyy");
                sdf.setCalendar(cal);
                cal.setTime(date);
                strDateStamp = sdf.format(date);
            }

            // XML output begins
            System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            System.out.println("<schedb generated=\"" + strDateStamp + "\" minutes-per-block=\"30\">");

            String strDept = "";

            //  Course
            NodeList nodeLstCourse = doc.getElementsByTagName("COURSE");
            for (int s = 0; s < nodeLstCourse.getLength(); s++) {
                Node nodeCourse = nodeLstCourse.item(s);
                if (nodeCourse.getNodeType() == Node.ELEMENT_NODE) {
                    Element elmntCourse = (Element) nodeCourse;
                    if ((strDept.length() == 0) || !strDept.equalsIgnoreCase(elmntCourse.getAttribute("dept"))) {
                        if (strDept.length() == 0) {
                        } else {
                            System.out.println("</dept>");
                        }
                        System.out.println("<dept abbrev=\"" + elmntCourse.getAttribute("dept") + "\"" + " name=\"" + elmntCourse.getAttribute("dept") + "\"" + ">");
                        strDept = elmntCourse.getAttribute("dept");
                    }
                    String strName = elmntCourse.getAttribute("name");
                    if (strName != null && strName.length() > 0) {
                        strName = strName.replace("&", "&amp;");
                    }
                    System.out.println("<course number=\"" + elmntCourse.getAttribute("num") + "\"" + " name=\"" + strName + "\"" + " min-credits=\"" + elmntCourse.getAttribute("credmin") + "\"" + " max-credits=\"" + elmntCourse.getAttribute("credmax") + "\"" + " grade-type=\"" + elmntCourse.getAttribute("gradetype") + "\"" + ">");

                    //  Section
                    NodeList nodeLstSection = elmntCourse.getElementsByTagName("SECTION");
                    for (int s2 = 0; s2 < nodeLstSection.getLength(); s2++) {
                        Node nodeSection = nodeLstSection.item(s2);
                        if (nodeSection.getNodeType() == Node.ELEMENT_NODE) {
                            Element elmntSection = (Element) nodeSection;

                            System.out.println("<section crn=\"" + elmntSection.getAttribute("crn") + "\"" + " number=\"" + elmntSection.getAttribute("num") + "\"" + " seats=\"" + elmntSection.getAttribute("seats") + "\"" + ">");

                            // Period

                            NodeList nodeLstPeriod = elmntSection.getElementsByTagName("PERIOD");
                            for (int s3 = 0; s3 < nodeLstPeriod.getLength(); s3++) {
                                Node nodePeriod = nodeLstPeriod.item(s3);
                                if (nodePeriod.getNodeType() == Node.ELEMENT_NODE) {
                                    Element elmntPeriod = (Element) nodePeriod;

                                    // Days
                                    String strDays = "";
                                    String strComma = "";
                                    NodeList nodeLstDay = elmntPeriod.getElementsByTagName("DAY");
                                    for (int s4 = 0; s4 < nodeLstDay.getLength(); s4++) {
                                        Element elmntDay = (Element) nodeLstDay.item(s4);
                                        NodeList nodeListDays = elmntDay.getChildNodes();
                                        String strVal = ((Node) nodeListDays.item(0)).getNodeValue();
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
                                    }

                                    String strEndTime = elmntPeriod.getAttribute("end");
                                    if (strEndTime != null && strEndTime.length() == 4) {
                                        strEndTime = convertTime(strEndTime);
                                    }

                                    System.out.println("<period type=\"" + elmntPeriod.getAttribute("type") + "\"" + " professor=\"" + elmntPeriod.getAttribute("instructor") + "\"" + " starts=\"" + strStartTime + "\"" + " ends=\"" + strEndTime + "\"" + strDays + "/>");

                                }
                            }

                            // End Section
                            System.out.println("</section>");
                        }
                    }

                    // End Course
                    System.out.println("</course>");

                }
            }
            System.out.println("</dept>");
            System.out.println("</schedb>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String convertTime(String s) {
        int intHours = Integer.parseInt(s.substring(0, 2));
        int intMins = Integer.parseInt(s.substring(2, 4));
        // convert times from 24hr to AM/PM
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR, intHours);
        cal.set(Calendar.MINUTE, intMins);
        Date date = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mma");
        sdf.setCalendar(cal);
        cal.setTime(date);
        return sdf.format(date);
    }
}
