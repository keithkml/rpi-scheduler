package edu.rpi.scheduler;

import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CourseXmlServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CourseXmlServlet.class.getName());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long ifModifiedSinceDate = request.getDateHeader("If-modified-since");
        if (ifModifiedSinceDate != -1)
            LOGGER.info("Requested if-modified-since " + new Date(ifModifiedSinceDate));
        String uri = request.getRequestURI();
        String subpath = uri.substring(8);
        if (!subpath.matches("\\d{6}.xml")) {
            response.sendError(404, "No such file " + subpath);
            return;
        }
        String semesterStr = subpath.substring(0, 6);
        int semesterInt;
        try {
            semesterInt = Integer.parseInt(semesterStr);
        } catch (NumberFormatException e) {
            response.sendError(404, "Invalid semester " + semesterStr + ", URL should end with '201101.xml'");
            return;
        }
        String requestedVersion = request.getParameter("v");
        CourseXml courseXml;
        if (requestedVersion != null) {
            long requestedVersionLong;
            try {
                requestedVersionLong = Long.parseLong(requestedVersion);
            } catch (NumberFormatException e) {
                response.sendError(404, "Invalid version " + requestedVersion);
                return;
            }
            courseXml = PMF.get().getPersistenceManager().getObjectById(CourseXml.class,
                                                                        requestedVersionLong);
            if (courseXml == null || courseXml.getSemester() != semesterInt) {
                response.sendError(404, "No such semester " + semesterInt + " and version " + requestedVersionLong);
                return;
            }
        } else {
            courseXml = getMostRecentXml(semesterInt);
            if (courseXml == null) {
                response.sendError(404, "No such semester " + semesterInt);
                return;
            }
        }

        long parseTimestampLong = courseXml.getParseTimestamp().getTime();
        if (parseTimestampLong < ifModifiedSinceDate) {
            response.setStatus(302);
            return;
        }
        
        response.setStatus(200);
        response.setDateHeader("Last-Modified", parseTimestampLong);
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");

        byte[] xml = courseXml.getCourseXml().getBytes();
        response.setContentLength(xml.length);
        int bufferSize = 10*1024;
        ServletOutputStream out = response.getOutputStream();
        for (int i = 0; i < xml.length; i += bufferSize) {
            out.write(xml, i, Math.min(xml.length-i,bufferSize));
            out.flush();
        }
    }

    @SuppressWarnings({"unchecked"})
    public static CourseXml getMostRecentXml(int semester) {
        Query query = PMF.get().getPersistenceManager().newQuery(CourseXml.class);
        query.setFilter("semester == :semester");
        query.setOrdering("lastModified descending");
        query.setRange(0,1);
        List<CourseXml> list = (List<CourseXml>) query.execute(semester);
        return list.isEmpty() ? null : list.get(0);
    }
}
