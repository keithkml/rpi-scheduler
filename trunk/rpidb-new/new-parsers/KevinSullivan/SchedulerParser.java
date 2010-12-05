import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

public class SchedulerParser extends DefaultHandler {
	ArrayList<Course> myCourses;
	
	private String tempVal;
	
	//to maintain context
	private Course tempCourse;
	private Section tempSection;
	private Period tempPeriod;

	public static void main(String argv[]) {
		SchedulerParser myParser = new SchedulerParser();
		File input, output;
		
		if (argv.length != 2) {
			System.out.println("SchedulerParser input.xml output.xml");
		}
		else {
			input = new File(argv[0]);
			output = new File(argv[1]);
			
			if (!input.exists()) {
				System.out.println("Doublecheck input file and try again");
			}
			else {
				myParser.parse(input);
				myParser.printData(output);
			}		
		}
	}
	
	private void parse(File input) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();
			parser.parse("in_example.xml", this);
		}
		catch(SAXException se) { se.printStackTrace(); }
		catch(ParserConfigurationException pce) { pce.printStackTrace(); }
		catch (IOException ie) { ie.printStackTrace(); }
	}
	
	public SchedulerParser() {
		myCourses = new ArrayList<Course>();
		tempCourse = new Course();
		tempSection = new Section();
		tempPeriod = new Period();
	}
	
	private void printData(File output){
		String tempDepartmentAbbrev;
		Department tempDepartment;
		ArrayList<Department> allDepartments = new ArrayList<Department>();
		ArrayList<String> departmentNames = new ArrayList<String>();
		
		try{
			FileWriter fstream = new FileWriter(output);
			BufferedWriter out = new BufferedWriter(fstream);

			for(int i1 = 0; i1 < myCourses.size(); i1++) {
				tempDepartmentAbbrev = myCourses.get(i1).getDept();
				if (!departmentNames.contains(tempDepartmentAbbrev)) { departmentNames.add(tempDepartmentAbbrev); }
			}
			
			for(int i2 = 0; i2 < departmentNames.size(); i2++) {
				tempDepartment = new Department(departmentNames.get(i2));
				allDepartments.add(tempDepartment);
			}
			
			for(int i3 = 0; i3 < allDepartments.size(); i3++) {
				for(int i4 = 0; i4 < myCourses.size(); i4++) {
					if(allDepartments.get(i3).getAbbrev().equals(myCourses.get(i4).getDept())) {
						allDepartments.get(i3).addCourse(myCourses.get(i4));
					}
				}
			}
			
			tempCourse = new Course();
			tempSection = new Section();
			tempPeriod = new Period();
			
			java.util.Date today = new java.util.Date();
			
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n");
			out.write("<schedb generated=\"" + today.toString() + "\" minutes-per-block=\"30\">" + "\n");
			
			for(int i5 = 0; i5 < allDepartments.size(); i5++) {
				out.write("  <dept abbrev=\"" + allDepartments.get(i5).getAbbrev() + 
				"\" name=\"" + allDepartments.get(i5).getName() + "\">" + "\n");
				
				for(int i6 = 0; i6 < allDepartments.get(i5).getCourseCount(); i6++) {
					tempCourse = allDepartments.get(i5).getCurrentCourse(i6);
					
					out.write("    <course number=\"" + tempCourse.getNum() + "\" name=\"" + tempCourse.getName()
					+ "\" min-credits=\"" + tempCourse.getMinCredits() + "\" max-credits=\"" + tempCourse.getMaxCredits()
					+ "\" grade-type=\"" + tempCourse.getGradeType() + "\">" + "\n");
					
					for(int i7 = 0; i7 < tempCourse.getSectionCount(); i7++) {
						tempSection = tempCourse.getCurrentSection(i7);
						
						out.write("      <section crn=\"" + tempSection.getCRN()
						+ "\" number=\"" + tempSection.getNum() + "\" seats=\"" + tempSection.getSeats()
						+ "\">" + "\n");
						
						for(int i8 = 0; i8 < tempSection.getPeriodCount(); i8++) {
							tempPeriod = tempSection.getCurrentPeriod(i8);
							
							out.write("        <period type=\"" + tempPeriod.getType()
							+ "\" professor=\"" + tempPeriod.getProfessor() + "\" days=\"" + tempPeriod.getDays()
							+ "\" starts=\"" + tempPeriod.getStart() + "\" ends=\"" + tempPeriod.getEnd()
							+ "\"/>" + "\n");
						}
						
						for(int i9 = 0; i9 < tempSection.getNoteCount(); i9++) {
							out.write("        <note>" + tempSection.getCurrentNote(i9) + "</note>" + "\n");
						}
						
						out.write("      </section>" + "\n");
					}
					
					out.write("    </course>" + "\n");
				}
				
				out.write("  </dept>" + "\n");
			}
			
			out.write("</schedb>");
			
			out.close();

		}
		catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		}
	}
	
	//Event Handlers
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//reset
		tempVal = "";
		if(qName.equalsIgnoreCase("COURSE")) {
			//create a new instance of employee
			tempCourse = new Course();
			tempCourse.setName(attributes.getValue("name"));
			tempCourse.setDept(attributes.getValue("dept"));
			tempCourse.setNumber(attributes.getValue("num"));
			tempCourse.setMinCredits(attributes.getValue("credmin"));
			tempCourse.setMaxCredits(attributes.getValue("credmax"));
			tempCourse.setGradeType(attributes.getValue("gradetype"));
		}
		if(qName.equalsIgnoreCase("SECTION")) {
			tempSection = new Section();
			tempSection.setCRN(attributes.getValue("crn"));
			tempSection.setNum(attributes.getValue("num"));
			tempSection.setStudents(attributes.getValue("students"));
			tempSection.setSeats(attributes.getValue("seats"));
		}
		if(qName.equalsIgnoreCase("PERIOD")) {
			tempPeriod = new Period();
			tempPeriod.setType(attributes.getValue("type"));
			tempPeriod.setInstructor(attributes.getValue("instructor"));
			tempPeriod.setStart(attributes.getValue("start"));
			tempPeriod.setEnd(attributes.getValue("end"));
			tempPeriod.setLocation(attributes.getValue("location"));
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		tempVal = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if(qName.equalsIgnoreCase("COURSE")) {
			myCourses.add(tempCourse);
		}else if (qName.equalsIgnoreCase("SECTION")) {
			tempCourse.addSection(tempSection);
		}else if (qName.equalsIgnoreCase("PERIOD")) {
			tempSection.addPeriod(tempPeriod);
		}else if (qName.equalsIgnoreCase("DAY")) {
			tempPeriod.addDay(tempVal);
		}
		else if(qName.equalsIgnoreCase("NOTE")) {
			tempSection.addNote(tempVal);
		}
	}
}