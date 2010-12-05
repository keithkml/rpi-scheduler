package edu.rpi.scheduler;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class CourseXml {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private int semester;

    /** Last modified date of source file on SIS */
    @Persistent
    private Date lastModified;

    @Persistent
    private Date parseTimestamp;

    @Persistent
    private Blob courseXml;

    public CourseXml(int semester, Date lastModified, Date parseTimestamp, byte[] courseXml) {
        this.semester = semester;
        this.lastModified = lastModified;
        this.parseTimestamp = parseTimestamp;
        this.courseXml = new Blob(courseXml);
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Blob getCourseXml() {
        return courseXml;
    }

    public int getSemester() {
        return semester;
    }
}
