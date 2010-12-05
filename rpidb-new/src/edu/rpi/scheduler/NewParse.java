package edu.rpi.scheduler;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

@SuppressWarnings({"AppEngineForbiddenCode"})
public class NewParse {
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        FileOutputStream out = new FileOutputStream(args[1]);
        out.write(new NewRpiParser().generateCourseXml(new URL(args[0]).openStream()));
        out.close();
    }
}
