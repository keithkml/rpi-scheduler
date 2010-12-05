import java.util.ArrayList;

public class Course {
	private ArrayList<Section> sections;
	private String name, gradeType, deptName, deptAbbrev;
	private int number, minCredits, maxCredits;
	
	public Course() {
		sections = new ArrayList<Section>();
		name = "NULL";
		gradeType = "NULL";
		deptName = "NULL";
		deptAbbrev = "NULL";
		number = -1;
		minCredits = -1;
		maxCredits = -1;
	}
	
	public int getSectionCount() {
		return sections.size();
	}
	
	public Section getCurrentSection(int i) {
		return sections.get(i);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setDept(String deptAbbrev) {
		this.deptAbbrev = deptAbbrev;
		
		if (deptAbbrev.equals("ADMN")) { this.deptName = "Administration"; }
		else { this.deptName = "Not configured!"; }
	}
	
	public void setGradeType(String gradeType) {
		this.gradeType = gradeType;
	}
	
	public void setNumber(String number) {
		this.number = Integer.parseInt(number);
	}
	
	public void setMinCredits(String minCredits) {
		this.minCredits = Integer.parseInt(minCredits);
	}
	
	public void setMaxCredits(String maxCredits) {
		this.maxCredits = Integer.parseInt(maxCredits);
	}
	
	public void addSection(Section newSection) {
		sections.add(newSection);
	}
	
	public String getDept() {
		return deptAbbrev;
	}
	
	public int getNum() {
		return number;
	}
	
	public int getMinCredits() {
		return minCredits;
	}
	
	public int getMaxCredits() {
		return maxCredits;
	}
	
	public String getName() {
		String tempName = name;
		
		tempName = tempName.replace("&", "&amp;");
		tempName = tempName.replace("-", " FOR");
	
		return tempName;
	}
	
	public String getGradeType() {
		if ( gradeType.length() == 0 ) { return "normal"; }
		
		return gradeType;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Course - ");
		sb.append("Name:" + name);
		sb.append(", ");
		sb.append("GradeType:" + gradeType);
		sb.append(", ");
		sb.append("DeptName:" + deptName);
		sb.append(", ");
		sb.append("Number:" + number);
		sb.append(".");
		
		return sb.toString();
	}
}
