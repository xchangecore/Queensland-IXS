Installing the HAVE Service:

1. Stop tomcat
2. Copy the com.saic.uicds.core.em.have-1.0.0.jar and com.saic.uicds.core.em.xmlbeans-have-1.0.0.jar into ServerApps\uicds\WEB-INF\lib
3. Create an xslt directory under WEB-INF/classes directory and copy HaveDigest.xsl to it
4. Copy applicationContext-have.xml to WEB-INF
5. Edit WEB-INF\web.xml and add /WEB-INF/applicationContext-have.xml to the contextConfigLocation
6. Edit WEB-INF\uicds-ws-servlet.xml and add the following line under the comment block with
   <!-- SCAN THE CLASSPATH FOR ENDPOINTS -->
   <context:component-scan base-package="com.saic.uicds.core.em.have.endpoint" />
7. Start tomcat
