import java.util.ArrayList;

public class Section {
	private ArrayList<String> notes;
	private ArrayList<Period> periods;
	private int crn, seats, students;
	private String number;	
	
	public Section(){
		periods = new ArrayList<Period>();
		notes = new ArrayList<String>();
		crn = -1;
		number = "NULL";
		seats = -1;
		students = -1;
	}
	
	public void addNote(String noteToAdd) {
		notes.add(noteToAdd);
	}
	
	public Period getCurrentPeriod(int i) {
		return periods.get(i);
	}
	
	public String getCurrentNote(int i) {
		return notes.get(i);
	}
	
	public int getPeriodCount() {
		return periods.size();
	}
	
	public int getNoteCount() {
		return notes.size();
	}
	
	public String getNum() {
		return number;
	}
	
	public int getCRN() {
		return crn;
	}
	
	public int getSeats() {
		return seats;
	}
	
	public int getStudents() {
		return students;
	}
	
	public void setCRN(String crn) {
		this.crn = Integer.parseInt(crn);
	}
	
	public void setNum(String number) {
		this.number = number;
	}
	
	public void setStudents(String students) {
		this.students = Integer.parseInt(students);
	}
	public void setSeats(String seats) {
		this.seats = Integer.parseInt(seats);
	}
	
	public void addPeriod(Period newPeriod) {
		periods.add(newPeriod);
	}
}
