UICDS XMPP Communication Extensions

This project contains two modules that are the basis for the core to core XMPP communications.
The extensions module contains code to help create the XMPP messages necessary to use the XMPP
pubsub extensions (XEP-0060) and the Openfire packet parsers for pubsub and UICDS specific 
messages.  The communications module contains the base classes for managing the XMPP connection,
roster, and file transfer.

Setup Instructions
1. Download Openfire XMPP server version 3.6.0a from http://www.igniterealtime.org/projects/openfire/index.jsp.

2. Install Openfire server

3. Start Openfire server

4. Configuring Openfire
   Start up Openfire:
   1.	Click Launch Admin 
   2.	Select English
   3.	Use defaults on Server Settings page.
   4.	Select Embedded Database on the Database Settings page
   5.	Select Default on Profile Settings page
   6.	Give the Admin a password on the Administrator Account page.  The recommended password is "ieoc&1549".  Using this password will ensure administrators can access the OpenFire for troubleshooting purposes.

   This will leave you a page to click to login to the Administrator Console.  Login.

   Under Server Settings on the left pane:
   1.	Select HTTP Binding
      a.	Select Disabled
      b.	Click Save Settings
   2.	Select Registration & Login
      a.	Select Disabled for Inband Account Registration
      b.	Select Disabled for Anonymous Login
      c.	Select Save Settings
   3.	Select Security Settings
      a.	Select Required under Client Connection Security
      b.	Select Required under Server Connection Security
      c.	Select Save Settings
   
   Under the Server Manager
   1.	Select System Properties and add or the following properties and set to the provided values:
   
   Property Name	Property Value
   xmpp.server.certificate.verify	false
   xmpp.server.dialback.enabled	true

5. Add uicds user:
   1. Select User/Groups tab at the top of the page
   2. Create a new user named "uicds" with a password of "uicds.1549"


Build Instructions:

1. Import the XMPP libraries into your local Maven repository.  These can be obtained from 
   http://www.igniterealtime.org/projects/smack/index.jsp.
  >mvn install:install-file -DgroupId=org.igniterealtime -DartifactId=smack -Dversion=3.2.1 -Dpackaging=jar -Dfile=C:\path\to\jar\file\smack.jar
  >mvn install:install-file -DgroupId=org.igniterealtime -DartifactId=smackx -Dversion=3.2.1 -Dpackaging=jar -Dfile=C:\path\to\jar\file\smackx.jar

2. Edit the test configuration files to point to your local installation of Openfire.
   a. Edit xmpp\communications\src\test\resources\contexts\test-coreConnectionTest.xml and
      xmpp\communications\src\test\resources\contexts\test-interestManagerTest.xml and change
      the following properties for the coreConnection bean by replacing "clash" with the name
      of your host.
      - server
      - servername
      - pubSubSvc

2. Run "mvn clean install" to build and test the XMPP classes.  Note that Openfire needs
   to be running when this is done because the unit tests for the communications module 
   use the XMPP server. 

