The University Scheduler Engine comes with "packed" JAR files to
accompany the standard JAR files. Using packed JAR files saves your users
download time, and saves you bandwidth. However, setting up packed JAR files
on your web server may be difficult.

It would be possible to set up Scheduler with packed JAR files on your own,
but documentation explaining how to do this will be provided in a later
release.

For now, here are some details on enabling packed JAR files yourself:
- If you use Apache HTTP Server, you should read
http://joust.kano.net/weblog/archive/2004/10/16/pack200-on-apache-web-server/

- If you use a Java Servlet enabled server like Tomcat or Weblogic, you should
  read http://java.sun.com/j2se/1.5.0/docs/guide/deployment/deployment-guide/pack200.html#theory