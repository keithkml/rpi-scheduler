#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <vector>
#include <locale>
#include <cstring>
#include <libxml/tree.h>

using namespace std;

bool iswhitespace(char c)
{
	return !isgraph(c);
}

bool iswhitespace(const char *s)
{
	if (*s=='\0')
		return true;
	return iswhitespace(*s) && iswhitespace(s+1);
}

bool iswhitespace(xmlChar *s)
{
	return iswhitespace((const char *)s);
}

string xmltype2string(int i)
{
	if (i==XML_ELEMENT_NODE)
		return "XML_ELEMENT_NODE";
	if (i==XML_ATTRIBUTE_NODE)
		return "XML_ATTRIBUTE_NODE";
	if (i==XML_TEXT_NODE)
		return "XML_TEXT_NODE";
	if (i==XML_CDATA_SECTION_NODE)
		return "XML_CDATA_SECTION_NODE";
	if (i==XML_ENTITY_REF_NODE)
		return "XML_ENTITY_REF_NODE";
	if (i==XML_ENTITY_NODE)
		return "XML_ENTITY_NODE";
	if (i==XML_PI_NODE)
		return "XML_PI_NODE";
	if (i==XML_COMMENT_NODE)
		return "XML_COMMENT_NODE";
	if (i==XML_DOCUMENT_NODE)
		return "XML_DOCUMENT_NODE";
	if (i==XML_DOCUMENT_TYPE_NODE)
		return "XML_DOCUMENT_TYPE_NODE";
	if (i==XML_DOCUMENT_FRAG_NODE)
		return "XML_DOCUMENT_FRAG_NODE";
	if (i==XML_NOTATION_NODE)
		return "XML_NOTATION_NODE";
	if (i==XML_HTML_DOCUMENT_NODE)
		return "XML_HTML_DOCUMENT_NODE";
	if (i==XML_DTD_NODE)
		return "XML_DTD_NODE";
	if (i==XML_ELEMENT_DECL)
		return "XML_ELEMENT_DECL";
	if (i==XML_ATTRIBUTE_DECL)
		return "XML_ATTRIBUTE_DECL";
	if (i==XML_ENTITY_DECL)
		return "XML_ENTITY_DECL";
	if (i==XML_NAMESPACE_DECL)
		return "XML_NAMESPACE_DECL";
	if (i==XML_XINCLUDE_START)
		return "XML_XINCLUDE_START";
	if (i==XML_XINCLUDE_END)
		return "XML_XINCLUDE_END";
	if (i==XML_DOCB_DOCUMENT_NODE)
		return "XML_DOCB_DOCUMENT_NODE";
	return "";
}

void printxmlAttrs(xmlNodePtr xp)
{
	if (xp==NULL)
		return;
	for (xmlAttrPtr xa=xp->properties; xa!=NULL; xa=xa->next)
	{
		xmlChar *v=xmlGetProp(xp, xa->name);
		cout << ' ' << xa->name << '=' << v;
	}
}

void printxmlrec(xmlNodePtr xp, int i)
{
	if (xp==NULL)
		return;
	if (xp->type!=XML_TEXT_NODE)
	{
		for (int n=0; n<i; ++n)
			cout << '\t';
		cout << xp->name << ' ' << xmltype2string(xp->type);
		printxmlAttrs(xp);
		cout << endl;
	}
	else
	{
		if (!iswhitespace(xp->content))
		{
			for (int n=0; n<i; ++n)
				cout << '\t';
			cout << xp->content << endl;
		}
	}
	for(xmlNodePtr xpc = xp->children; xpc != NULL; xpc = xpc->next)
	{
		printxmlrec(xpc, i+1);
	}
}

//These get rid of the compiler errors about converting xmlChars to chars and otherwise, and xmlChars are just UTF-8 strings, so nothing should go wrong
int strcmp(const xmlChar *s1, const xmlChar *s2)
{
	return strcmp((const char *)s1, (const char *)s2);
}

int strcmp(const char *s1, const xmlChar *s2)
{
	return strcmp(s1, (const char *)s2);
}

int strcmp(const xmlChar *s1, const char *s2)
{
	return strcmp((const char *)s1, s2);
}

xmlDocPtr xmlNewDoc(const char *version)
{
	return xmlNewDoc((const xmlChar *)version);
}

xmlNodePtr xmlNewNode(const char *name)
{
	return xmlNewNode(NULL, (const xmlChar *)name);
}

xmlAttrPtr xmlSetProp(xmlNodePtr node, const char *name, const char *value)
{
	return xmlSetProp(node, (const xmlChar *)name, (const xmlChar *)value);
}

xmlAttrPtr xmlSetProp(xmlNodePtr node, const xmlChar *name, const char *value)
{
	return xmlSetProp(node, name, (const xmlChar *)value);
}

xmlAttrPtr xmlSetProp(xmlNodePtr node, const char *name, const xmlChar *value)
{
	return xmlSetProp(node, (const xmlChar *)name, value);
}

char *xmlGetProp(xmlNodePtr node, const char *name)
{
	return (char *)(xmlGetProp(node, (const xmlChar *)name));
}

xmlNodePtr xmlNewText(const char *content)
{
	return xmlNewText((const xmlChar *)content);
}

const char *unabbrev(const char *s)
{
	if (!strcmp(s, "LEC"))
		return "lecture";
	if (!strcmp(s, "REC"))
		return "recitation";
	if (!strcmp(s, "SEM"))
		return "seminar";
	if (!strcmp(s, "   "))
		return "unknown";
	if (!strcmp(s, "TES"))
		return "unknown"; //Should be test
	return s; //This should be expanded
}

const char *schedgradetype(const char *s)
{
	if (!strcmp(s, ""))
		return "normal";
	if (!strcmp(s, "Satisfactory/Unsatisfactory"))
		return "pass-fail";
	return "";
}

const char *getdays(xmlNodePtr xpd)
{
	const char *daychars[]={
		"",
		"mon",
		"tue",
		"mon,tue",
		"wed",
		"mon,wed",
		"tue,wed",
		"mon,tue,wed",
		"thu",
		"mon,thu",
		"tue,thu",
		"mon,tue,thu",
		"wed,thu",
		"mon,wed,thu",
		"tue,wed,thu",
		"mon,tue,wed,thu",
		"fri",
		"mon,fri",
		"tue,fri",
		"mon,tue,fri",
		"wed,fri",
		"mon,wed,fri",
		"tue,wed,fri",
		"mon,tue,wed,fri",
		"thu,fri",
		"mon,thu,fri",
		"tue,thu,fri",
		"mon,tue,thu,fri",
		"wed,thu,fri",
		"mon,wed,thu,fri",
		"tue,wed,thu,fri",
		"mon,tue,wed,thu,fri"};
	int d=0;
	while (xpd!=NULL)
	{
		if (xpd->type==XML_ELEMENT_NODE && !strcmp(xpd->name, "DAY"))
		{
			xmlChar *n=xmlNodeGetContent(xpd);
			if (n!=NULL && *n>='0' && *n<='4')
				d+=1<<(*n-'0');
			xmlFree(n);
		}
		xpd=xpd->next;

	}
	return daychars[d];
}

char *timeconv(char *t)
{
	bool ispm=false;
	bool islong=false;
	char *tc=(char *)malloc(9); //The most space we'll ever need
	if (!strcmp(t, "** TBA **"))
	{
		strcpy(tc, t);
		return tc;
	}
	//Doesn't handle 0000 properly, but it doesn't matter
	if (t[0]=='2')
	{
		ispm=true;
		if (t[1]>='2')
		{
			islong=true;
			tc[0]='1';
			tc[1]=t[1]-2;
		}
		else
		{
			tc[0]=t[1]+8;
		}
	}
	else if (t[0]=='1')
	{
		if (t[1]>='3')
		{
			ispm=true;
			tc[0]=t[1]-2;
		}
		else if (t[1]=='2')
		{
			islong=true;
			ispm=true;
			tc[0]='1';
			tc[1]='2';
		}
		else
		{
			islong=true;
			tc[0]='1';
			tc[1]=t[1];
		}
	}
	else if (t[0]=='0')
	{
		tc[0]=t[1];
	}
	if (islong)
	{
		tc[2]=':';
		tc[3]=t[2];
		tc[4]=t[3];
		if (ispm)
			tc[5]='P';
		else
			tc[5]='A';
		tc[6]='M';
		tc[7]='\0';
	}
	else
	{
		tc[1]=':';
		tc[2]=t[2];
		tc[3]=t[3];
		if (ispm)
			tc[4]='P';
		else
			tc[4]='A';
		tc[5]='M';
		tc[6]='\0';
	}
	return tc;
}

void addperiod(xmlNodePtr xpp, xmlNodePtr xps)
{
	char *type=xmlGetProp(xpp, "type");
	char *instructor=xmlGetProp(xpp, "instructor");
	char *start=xmlGetProp(xpp, "start");
	char *starttime=timeconv(start);
	char *end=xmlGetProp(xpp, "end");
	char *endtime=timeconv(end);
	const char *days=getdays(xpp->children);
	xmlNodePtr p=xmlNewNode("period");
	xmlSetProp(p, "type", unabbrev(type));
	xmlSetProp(p, "professor", instructor);
	xmlSetProp(p, "days", days);
	xmlSetProp(p, "starts", starttime);
	xmlSetProp(p, "ends", endtime);
	xmlNodePtr i=xmlNewText("\n        ");
	xmlAddChild(xps, i);
	xmlAddChild(xps, p);
	free(starttime);
	free(endtime);
	xmlFree(type);
	xmlFree(instructor);
	xmlFree(start);
	xmlFree(end);
}

vector<xmlChar *> addsection(xmlNodePtr xps, xmlNodePtr xpc)
{
	char *crn=xmlGetProp(xps, "crn");
	char *num=xmlGetProp(xps, "num");
	char *seats=xmlGetProp(xps, "seats");
	xmlNodePtr s=xmlNewNode("section");
	xmlSetProp(s, "crn", crn);
	xmlSetProp(s, "number", num);
	xmlSetProp(s, "seats", seats);
	vector<xmlChar *> notes;
	for (xmlNodePtr xpp=xps->children; xpp!=NULL; xpp=xpp->next)
	{
		if (xpp->type==XML_ELEMENT_NODE && !strcmp(xpp->name, "PERIOD"))
			addperiod(xpp, s);
		if (xpp->type==XML_ELEMENT_NODE && !strcmp(xpp->name, "NOTE"))
		{
			xmlChar *n=xmlNodeGetContent(xpp);
			notes.push_back(n);
		}
	}
	xmlNodePtr is=xmlNewText("\n      ");
	xmlAddChild(s, is);
	xmlNodePtr ic=xmlNewText("\n      ");
	xmlAddChild(xpc, ic);
	xmlAddChild(xpc, s);
	xmlFree(crn);
	xmlFree(num);
	xmlFree(seats);
	return notes;
}

void addcourse(xmlNodePtr xpc, map<string, xmlNodePtr> &m)
{
	char *name=xmlGetProp(xpc, "name");
	char *dept=xmlGetProp(xpc, "dept");
	char *num=xmlGetProp(xpc, "num");
	char *credmin=xmlGetProp(xpc, "credmin");
	char *credmax=xmlGetProp(xpc, "credmax");
	char *gradetype=xmlGetProp(xpc, "gradetype");
	string d(dept);
	if (m[d]==NULL)
	{
		m[d]=xmlNewNode("dept");
		xmlSetProp(m[d], "abbrev", dept);
		xmlSetProp(m[d], "name", unabbrev(dept));
	}
	xmlNodePtr c=xmlNewNode("course");
	xmlSetProp(c, "number", num);
	xmlSetProp(c, "name", name);
	xmlSetProp(c, "min-credits", credmin);
	xmlSetProp(c, "max-credits", credmax);
	xmlSetProp(c, "grade-type", schedgradetype(gradetype));
	vector<xmlChar *> notes;
	for (xmlNodePtr xps=xpc->children; xps!=NULL; xps=xps->next)
	{
		if (xps->type==XML_ELEMENT_NODE && !strcmp(xps->name, "SECTION"))
		{
			vector<xmlChar *> v=addsection(xps, c);
			notes.insert(notes.end(), v.begin(), v.end());
		}
	}
	for (unsigned int i=0; i<notes.size(); ++i)
	{
		xmlNodePtr xpn=xmlNewNode("note");
		xmlNodeSetContent(xpn, notes[i]);
		xmlNodePtr in=xmlNewText("\n      ");
		xmlAddChild(c, in);
		xmlAddChild(c, xpn);
		xmlFree(notes[i]);
	}
	xmlNodePtr ic=xmlNewText("\n    ");
	xmlAddChild(c, ic);
	xmlNodePtr im=xmlNewText("\n    ");
	xmlAddChild(m[d], im);
	xmlAddChild(m[d], c);
	xmlFree(name);
	xmlFree(dept);
	xmlFree(num);
	xmlFree(credmin);
	xmlFree(credmax);
	xmlFree(gradetype);
}

xmlDocPtr buildschedb(xmlDocPtr doc)
{
	//The top node should be CourseDB, so check for it
	xmlNodePtr xp=xmlDocGetRootElement(doc);
	if (strcmp(xp->name, "CourseDB"))
	{
		//cout << xp->name;
		return NULL;
	}
	xmlDocPtr newdoc=xmlNewDoc("1.0"); //Create a new xml 1.0 document
	xmlNewDocProp(newdoc, (const xmlChar *)"encoding", (const xmlChar *)"UTF-8");
	xmlNodePtr schedb=xmlNewNode("schedb");
	xmlSetProp(schedb, "generated", "Mon Aug 23 19:19:11 EDT 2010"); //I'll do this right later
	xmlSetProp(schedb, "minutes-per-block", "30");
	map<string, xmlNodePtr> deptmap;
	for (xmlNodePtr xpc=xp->children; xpc!=NULL; xpc=xpc->next)
	{
		if (xpc->type==XML_ELEMENT_NODE && !strcmp(xpc->name, "COURSE")) //Only add courses
			addcourse(xpc, deptmap);
	}
	for (map<string, xmlNodePtr>::iterator i=deptmap.begin(); i!=deptmap.end(); ++i)
	{
		xmlNodePtr id=xmlNewText("\n  ");
		xmlAddChild(i->second, id);
		xmlNodePtr ir=xmlNewText("\n  ");
		xmlAddChild(schedb, ir);
		xmlAddChild(schedb, i->second);
	}
	xmlNodePtr is=xmlNewText("\n");
	xmlAddChild(schedb, is);
	xmlDocCopyNode(schedb, newdoc, 1);
	xmlDocSetRootElement(newdoc, schedb);
	return newdoc;
}

int main(int argc, char *argv[])
{
	if (argc!=3)
	{
		cout << "Transforms rpi's schedule format into RPI Scheduler's format\n"
			<< "Usage: " << argv[0] << "infile outfile\n";
		return 1;
	}
	xmlDocPtr doc;
	doc=xmlParseFile(argv[1]);
	xmlDocPtr newdoc=buildschedb(doc);
	xmlSaveFile(argv[2], newdoc);
	xmlFreeDoc(doc);
	xmlFreeDoc(newdoc);
	return 0;
}
