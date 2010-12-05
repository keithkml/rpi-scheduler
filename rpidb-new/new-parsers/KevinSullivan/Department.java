import java.util.ArrayList;

public class Department {
	private ArrayList<Course> courses;
	private String name, abbrev;
	
	public Department(String abbrev){
		courses = new ArrayList<Course>();
		this.abbrev = abbrev;
		
		if (abbrev.equals("ADMN")) { name = "Administration"; }
		else if (abbrev.equals("ARCH")) { name = "Architecture"; }
		else if (abbrev.equals("ARTS")) { name = "Arts"; }
		else if (abbrev.equals("ASTR")) { name = "Astronomy"; }
		else if (abbrev.equals("BCBP")) { name = "Biochemistry and Biophysics"; }
		else if (abbrev.equals("BIOL")) { name = "Biology"; }
		else if (abbrev.equals("BMED")) { name = "Biomedical Engr."; }
		else if (abbrev.equals("CHEM")) { name = "Chemistry"; }
		else if (abbrev.equals("CHME")) { name = "Chemical Engr."; }
		else if (abbrev.equals("CIVL")) { name = "Civil Engr."; }
		else if (abbrev.equals("COGS")) { name = "Cognitive Science"; }
		else if (abbrev.equals("COMM")) { name = "Communication"; }
		else if (abbrev.equals("CSCI")) { name = "Computer Science"; }
		else if (abbrev.equals("ECON")) { name = "Economics"; }
		else if (abbrev.equals("ECSE")) { name = "Electrical, Computer, and Systems Engr."; }
		else if (abbrev.equals("ENGR")) { name = "General Engineering"; }
		else if (abbrev.equals("ENVE")) { name = "Environmental Engr."; }
		else if (abbrev.equals("EPOW")) { name = "Electric Power Engr."; }
		else if (abbrev.equals("ERTH")) { name = "Earth & Environmental Sciences"; }
		else if (abbrev.equals("ESCI")) { name = "Engineering Science"; }
		else if (abbrev.equals("IENV")) { name = "Interdisciplinary Environmental"; }
		else if (abbrev.equals("IHSS")) { name = "Interdisciplinary H&amp;SS"; }
		else if (abbrev.equals("ISCI")) { name = "Interdisciplinary Science"; }
		else if (abbrev.equals("ISYE")) { name = "Industrial and Management Engr."; }
		else if (abbrev.equals("ITWS")) { name = "Information Technology and Web Science"; }
		else if (abbrev.equals("LGHT")) { name = "Lighting"; }
		else if (abbrev.equals("LITR")) { name = "Literature"; }
		else if (abbrev.equals("MANE")) { name = "Mechanical, Aerospace, and Nuclear Engr."; }
		else if (abbrev.equals("MATH")) { name = "Mathematics"; }
		else if (abbrev.equals("MATP")) { name = "Math Prog., Prob., and Stats."; }
		else if (abbrev.equals("MGMT")) { name = "Management"; }
		else if (abbrev.equals("MTLE")) { name = "Materials Science and Engr."; }
		else if (abbrev.equals("PHIL")) { name = "Philosophy"; }
		else if (abbrev.equals("PHYS")) { name = "Physics"; }
		else if (abbrev.equals("PSYC")) { name = "Psychology"; }
		else if (abbrev.equals("STSH")) { name = "Science and Technology Studies"; }
		else if (abbrev.equals("STSS")) { name = "Science and Technology Studies"; }
		else if (abbrev.equals("USAF")) { name = "Air Force ROTC"; }
		else if (abbrev.equals("USAR")) { name = "Army ROTC"; }
		else if (abbrev.equals("USNA")) { name = "Navy ROTC"; }
		else if (abbrev.equals("WRIT")) { name = "Writing"; }
		else { name = "Missing ABBREV -> NAME conversion!"; }
	}
	
	public Course getCurrentCourse(int i) {
		return courses.get(i);
	}
	
	public int getCourseCount() {
		return courses.size();
	}
	
	public String getAbbrev() {
		return abbrev;
	}
	
	public String getName() {
		return name;
	}
	
	public void addCourse(Course newCourse) {
		courses.add(newCourse);
	}
}
