Attached is a script to form old-style data from the new sis xml format.  I 
haven't done real rigorous testing so I suggest people double check a 
schedule with sis before they get committed to it.  

Some classes were marked as TBA in the time field.  As leaving it as TBA 
killed scheduler, I went and dropped those classes.  Most of them were 
either ADM or EPOW.  The code is really messy and I didn't comment much at 
all.  But it's pretty short so it should still be reasonably understandable. 

My script doesn't output anything even remotely human readable.  A quick way
to slightly improve that on linux is:
sed -i "s/>/>\n/g" output.xml
