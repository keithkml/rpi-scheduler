package edu.rpi.scheduler;

import org.xml.sax.SAXException;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ParserServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int semesterInt = 201101;
        CourseXml mostRecent = CourseXmlServlet.getMostRecentXml(semesterInt);
        String url = "http://sis.rpi.edu/reg/rocs/" + semesterInt + ".xml";
        URLConnection conn = new URL(url).openConnection();
        if (mostRecent != null)
            conn.setIfModifiedSince(mostRecent.getLastModified().getTime());

        long lastModified = conn.getLastModified();

        String lastModifiedString = DateFormat.getDateTimeInstance().format(new Date(lastModified));
        if (mostRecent != null) {
            if (lastModified <= mostRecent.getLastModified().getTime()) {
                LOGGER.info("File was not modified on SIS - " + lastModifiedString);
                // nothing changed
                resp.setStatus(302);
                return;
            }
        }

        byte[] courseXml;
        try {
            courseXml = new NewRpiParser().generateCourseXml(conn.getInputStream());
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error parsing " + url, e);
            resp.sendError(500, "Error parsing " + url);
            return;
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Error parsing " + url, e);
            resp.sendError(500, "Error parsing " + url);
            return;
        }

        LOGGER.info("Committing " + courseXml.length/1024 + "kb file, modified at " + lastModifiedString);

        CourseXml semester = new CourseXml(semesterInt, new Date(lastModified), new Date(), courseXml);
        PersistenceManager pm = PMF.get().getPersistenceManager();
        pm.makePersistent(semester);
    }
}
