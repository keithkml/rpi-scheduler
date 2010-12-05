from elementtree.ElementTree import *
from datetime import *


#get the full name of a dept abbrev
def getFullName(abbrev):
    return {'ARCH' : 'Architecture',
            'ADMN' : 'Administration',
            'LGHT' : 'Lighting',
            'BMED' : 'Biomedical Engineering',
            'CHME' : 'Chemical Engineering',
            'CIVL' : 'Civil Engineering',
            'ECSE' : 'Electrical, Computer, and Systems Engineering',
            'ENGR' : 'General Engineering',
            'ENVE' : 'Environmental Engineering',
            'EPOW' : 'EPOW',
            'ESCI' : 'Engineering Science',
            'ISYE' : 'Industrial and Systems Engineering',
            'MANE' : 'Mechanical, Aerospace, and Nuclear Engineering',
            'MTLE' : 'Materials Science and Engineering',
            'ARTS' : 'Arts',
            'COMM' : 'Communication',
            'IHSS' : 'Interdisciplinary Humanities and Social Sciences',
            'LANG' : 'Foreign Languages and Literature',
            'LITR' : 'Literature',
            'PHIL' : 'Philosophy',
            'STSH' : 'Science and Technology Studies (Humanities Courses)',
            'WRIT' : 'Writing',
            'COGS' : 'Cognitive Science',
            'ECON' : 'Economics',
            'IHSS' : 'Interdisciplinary Humanities and Social Science',
            'PSYC' : 'Psychology',
            'STSS' : 'Science and Technology Studies (Social Sciences Courses)',
            'ITWS' : 'Information Technology and Web Science',
            'MGMT' : 'Management',
            'ASTR' : 'Astronomy',
            'BCBP' : 'Biochemistry and Biophysics',
            'BIOL' : 'Biology',
            'CHEM' : 'Chemistry',
            'CISH' : 'Computer Science at Hartford',
            'CSCI' : 'Computer Science',
            'ISCI' : 'Interdisciplinary Science',
            'ERTH' : 'Earth and Environmental Science',
            'MATH' : 'Mathematics',
            'MATP' : 'Mathematical Programming, Probability, and Statistics',
            'PHYS' : 'Physics',
            'IENV' : 'Interdisciplinary Environmental Courses',
            'USAF' : 'Aerospace Studies (Air Force ROTC)',
            'USAR' : 'Military Science (Army ROTC)',
            'USNA' : 'Naval Science (Navy ROTC)',
            'NSST' : 'Natural Science for School Teachers'}[abbrev]

def getOldType(newType):
    return {'LEC' : 'lecture',
            'LAB' : 'lab',
            'STU' : 'studio',
            'REC' : 'recitation',
            'SEM' : 'seminar',
            'TES' : 'test',
            '   ' : 'lecture'}[newType]



#reformat the time to the old format
def timeFormat(inTime):
    try:
        t= time(int(inTime)/100,int(inTime)%100)
        return t.strftime('%I:%M %p')
    except:  #** TBA ** = me angry
        return None


tree = parse("201101.xml")
fromRoot = tree.getroot()

toRoot = Element("schedb", generated=datetime.now().ctime())
toRoot.set("minutes-per-block", "30")


fromCourses = fromRoot.findall('COURSE')



for course in fromCourses:
    die=False
    hasCurDept = not (toRoot.find('dept') == None)
    if hasCurDept:
        hasCurDept=False
        for curDept in toRoot.findall('dept'):
            if curDept.get('abbrev') == course.get('dept'):
                hasCurDept = True
                deptEle = curDept

    if not hasCurDept:
        deptEle=SubElement(toRoot, "dept", abbrev=course.get('dept'), name=getFullName(course.get('dept')))
    courseEle = SubElement(deptEle, "course", number=course.get('num'), name=course.get('name'))
    courseEle.set('min-credits',course.get('credmin'))
    courseEle.set('max-credits',course.get('credmax'))
    if course.get('gradetype')=='':
        courseEle.set('grade-type', 'normal')
    else:
        courseEle.set('grade-type',course.get('gradetype'))
    
    for section in course.findall('SECTION'):
        secEle = SubElement(courseEle, 'section', crn=section.get('crn'), number=section.get('num'), seats=section.get('seats'))
        for period in section.findall('PERIOD'):
            start=timeFormat(period.get('start'))
            end=timeFormat(period.get('end'))

            if start==None or end == None:
                die=True
                break

            perEle=SubElement(secEle, 'period', type=getOldType(period.get('type')), 
                              professor=period.get('instructor'), starts=start, ends=end)

            days=''
            for day in period.findall('DAY'):
                days+=('mon','tue','wed','thu','fri')[int(day.text)]
                days+=','
            days=days.rstrip(',')

            perEle.set('days', days)
    if die:
        #invalid entry, kill it so we don't kill scheduler
        deptEle.remove(courseEle)



ElementTree(toRoot).write("output.xml")




