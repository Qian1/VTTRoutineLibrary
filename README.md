# VTT Routine Library for Android 
 
 
## 1. Introduction 
 
VTT Routine Library for Android provides routine detection methods. A routine comprises of location and application combinations: 
 
* Applications 
* Location (Cell ID, GPS) 
 
 
## 2. Installation and configuration 
 
* Client/RoutineClient/ - contains a simple example application that uses the library. 
* Client/RoutineLibrary/ - contains the library code. 
* Server/ActivityLogger/ - contains the example server-side component for receiving raw log data as well as routine data and for storing it to a PostgreSQL database. 
 
1. If not installed already, install [Eclipse](http://www.eclipse.org/downloads/) and [Android tools](http://developer.android.com/sdk/installing.html). Android API level 10 and higher are supported, so it might be a good idea to start with API level 10 (2.3.3). 
 
2. If not created already, create a new workspace in Eclipse. 
 
3. Import RoutineClient into the workspace: 
 
        "File" -> "Import..." -> "General" -> "Existing Projects into Workspace" -> click "Next >" 
 
        "Select root directory:" -> click "Browse..." -> navigate to "RoutineClient" -> press "OK" -> press "Finish" 
 
4. Import RoutineLibrary into the workspace, do the same as in the previous step, but select RoutineLibrary instead. 
 
5. Connect your Android device to the computer (if the device does not show up, you might need to install device specific debug drivers, for example, for HTC devices [HTC Sync](http://www.htc.com/www/support/) needs to be installed). 
 
6. Clean and build both projects. 
 
7. Run RoutineClient in the mobile device (emulator not supported): 
 
        Right click on the "RoutineClient" -> "Run As" (or "Debug As", if you prefer) -> "Android Application" 
 
8. For ActivityLogger, you should also have [Tomcat](http://tomcat.apache.org/) and [PostgreSQL](http://www.postgresql.org/) installed and running on some server. 
 
 
## 3. Documentation 
 
Usage, see RoutineClient: 
 
* fi.vtt.routineclient.RoutineClientActivity.java 
* fi.vtt.routineclient.RoutineClientService.java 
 
RoutineClientActivity is the main entry point in this example. 
 
For more description, see the documentation: 
 
	Javadoc - see Documentation/Javadoc/index.html 
 
	Developer_notes.rtf 
 
 
## 4. Known problems and solutions 
 
If you get messages, such as: 
 
	"Must override a super class." 
 
	"Android requires compiler compliance level 5.0 or 6.0. Found '1.4' instead. Please use Android Tools > Fix Project Properties." 
 
Set compliance level to, for example, 1.6: 
 
	"Project" -> "Properties" -> "Java Compiler" -> "Compiler compliance level: 1.6" 
 
Clean and build both projects again. 
 
 
## 5. License 
 
VTT Routine Library is available under the BSD License. 
 
 
## 6. Contact 
 
No particular support is provided, but questions, suggestions etc. can be emailed to [cauit@vtt.fi](mailto:cauit@vtt.fi). 
 
 