package edu.rpi.scheduler;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;

public class DiffServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ap = req.getParameter("a");
        String bp = req.getParameter("b");
        long a = Long.parseLong(ap);
        long b = Long.parseLong(bp);

        PersistenceManager pm = PMF.get().getPersistenceManager();
        CourseXml ao = pm.getObjectById(CourseXml.class, a);
        CourseXml bo = pm.getObjectById(CourseXml.class, b);

        diff_match_patch diff = new diff_match_patch();
        LinkedList<Diff> diffs = diff.diff_main(new String(ao.getCourseXml().getBytes(), "UTF-8"),
                                                new String(bo.getCourseXml().getBytes(), "UTF-8"));
        diff.diff_cleanupSemantic(diffs);
        String result = prettyPrint(diffs);
        resp.setContentType("text/html");
        resp.getWriter().write(result);
    }

    private String prettyPrint(LinkedList<Diff> diffs) {
        StringBuilder html = new StringBuilder();
        int i = 0;
        html.append("<html><head><style type=text/css>body { font-family: monospace }</style></head><body>");
        for (Diff aDiff : diffs) {
            String text = aDiff.text.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;");
            switch (aDiff.operation) {
                case INSERT:
                    html.append("<INS STYLE=\"background:#a6dda6;\">").append(text).append("</INS>");
                    break;
                case DELETE:
                    html.append("<DEL STYLE=\"background:#bb9696;\">").append(text).append("</DEL>");
                    break;
                case EQUAL:
                    String[] lines = text.split("\\n");
                    html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(lines[0])
                            .append("</SPAN>");
                    if (text.contains("\n"))
                        html.append("<br>");
                    if (lines.length > 1) {
                        html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(lines[lines.length-1])
                                .append("</SPAN>");
                        if (text.endsWith("\n"))
                            html.append("<br>");
                    }
                    break;
            }
            if (aDiff.operation != Operation.DELETE) {
                i += aDiff.text.length();
            }
        }
        return html.toString();
    }
}
