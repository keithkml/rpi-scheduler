import java.util.ArrayList;

public class Period {
	private ArrayList<Integer> days;
	private String type, professor, location;
	private int starts, ends;
	
	public Period(){
		days = new ArrayList<Integer>();
		type = "NULL";
		professor = "NULL";
		starts = -1;
		ends = -1;
		location = "NULL";
	}
	
	public String getType() {
		return type;
	}
	
	public String getProfessor() {
		return professor;
	}
	
	public String getDays() {
		String theDayString = "";
		
		for (int i = 0; i < days.size(); i++) {
			if (days.get(i) == 0) {
				theDayString += "mon,";
			}
			else if (days.get(i) == 1) {
				theDayString += "tue,";
			}
			else if (days.get(i) == 2) {
				theDayString += "wed,";
			}
			else if (days.get(i) == 3) {
				theDayString += "thu,";
			}
			else if (days.get(i) == 4) {
				theDayString += "fri,";
			}
		}
		
		if (theDayString.length() > 0) {
			theDayString = theDayString.substring(0, theDayString.length() - 1);
		}
		
		return theDayString;
	}
	
	public String getStart() {
		if (starts == -666) { return "** TBA **"; }
		
		String theStartString = "";
		
		int shour = starts/100;
		int smin = starts-(starts/100*100);
		int dn = 1;

		if ( shour < 1 || shour > 24 || smin < 0 || smin > 59 )
			System.out.println("Invalid Input. Try Again.");
		if ( shour > 12){
			shour = shour-12;
			dn = 2;
		}
		
		if ( smin == 0 ) {
			theStartString = shour + ":" + smin + "0";
		}
		else { theStartString = shour + ":" + smin; }

		if (dn == 1){
			theStartString += "AM";
		} else if (dn == 2){ 
			theStartString += "PM";
		}
		
		return theStartString;
	}
	
	public String getEnd() {
		if (ends == -666) { return "** TBA **"; }
	
		String theEndString = "";
		
		int shour = ends/100;
		int smin = ends-(ends/100*100);
		int dn = 1;
		
		if ( shour < 1 || shour > 24 || smin < 0 || smin > 59 )
			System.out.println("Invalid Input. Try Again.");
		if ( shour > 12){
			shour = shour-12;
			dn = 2;
		}
		
		if ( smin == 0 ) {
			theEndString = shour + ":" + smin + "0";
		}
		else { theEndString = shour + ":" + smin; }

		if (dn == 1){
			theEndString += "AM";
		} else if (dn == 2){ 
			theEndString += "PM";
		}
		
		return theEndString;
	}
	
	public void setEnd(String ends) {
		if (ends.equals("** TBA **")) { this.ends = -666; }
		else { this.ends = Integer.parseInt(ends); }
	}
	
	public void setStart(String starts) {
		if (starts.equals("** TBA **")) { this.starts = -666; }
		else { this.starts = Integer.parseInt(starts); }
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setInstructor(String professor) {
		this.professor = professor;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public void addDay(String newDay) {
		days.add(Integer.parseInt(newDay));
	}
}