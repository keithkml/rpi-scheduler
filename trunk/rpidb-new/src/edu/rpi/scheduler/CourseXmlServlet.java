package edu.rpi.scheduler;

import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class CourseXmlServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String subpath = uri.substring(8);
        int subpathInt;
        try {
            subpathInt = Integer.parseInt(subpath);
        } catch (NumberFormatException e) {
            response.sendError(404, "No such semester " + subpath);
            return;
        }
        Semester result = getMostRecentXml(subpathInt);
        if (result == null) {
            response.sendError(404, "No such semester " + subpath);
            return;
        }
        
        response.setStatus(200);
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");

        byte[] xml = result.getCourseXml().getBytes();
        response.setBufferSize(1000);
        ServletOutputStream out = response.getOutputStream();
        for (int i = 0; i < xml.length; i += 1000) {
            out.write(xml, i, Math.min(xml.length,i+1000));
        }
    }

    @SuppressWarnings({"unchecked"})
    public static Semester getMostRecentXml(int semester) {
        Query query = PMF.get().getPersistenceManager().newQuery(Semester.class);
        query.setFilter("semester == :semester");
        query.setOrdering("lastModified descending");
        query.setRange(0,1);
        List<Semester> list = (List<Semester>) query.execute(semester);
        return list.isEmpty() ? null : list.get(0);
    }
}
