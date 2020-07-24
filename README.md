# Note-Knight
Notepad with file encryption! (and some other small tweaks)

Build the file using Maven! It will automatically download the SCrypt library from its source. JDK 11 or higher is required.

Utilize the following command to have Maven compile the program and build a JAR:

    mvn assembly:assembly

Just want to try out the application? You can download it from [here](https://github.com/this-is-forever/Note-Knight/raw/master/note-knight-1.1-jar-with-dependencies.jar).

You should be able to open the JAR file from Windows Explorer (or your OS's equivalent). If not, the following command works just as well:

javaw -classpath note-knight-1.1-jar-with-dependencies.jar application.Main

You can configure Windows to open .nkx files with launcher.bat, that way you can open right into Note Knight from Windows Explorer!
