# Java Concurrency Tracer (jctrace)

The Java Concurrency Tracer allows you to find deadlocks in your Java application.

The Java Concurrency Tracer instruments a Java application to find **potential** deadlocks in the system.
Jctrace will create a database of all taken and release locks in the application.
In a second step jctrace will calculate if any two locks have any threads, where the lock order is inversed, which will flag an potential deadlock.


**Note:** Not every flaged potential deadlock, will be a real deadlock in you system. E.g. if you can guarantee that the found lock orders can not happen at the same time, the potential deadlock can not happen. But it would be no good concurrency design to rely on these fragile terms.

## Analysis details

Jctrace will instrument and measure the following items:
* `synchornized` keyword
  * For any Obejct or Class instance
* `synchornized` methods
* `java.util.concurrent.locks.ReentrantLock`
* `java.util.concurrent.locks.ReentrantReadWriteLock`
* `java.util.concurrent.locks.StampedLock`

The jctrace is currently only tested with Java 8.

Jctrace could also create stacktraces of found potential deadlocks to ease the fix of the underlying problem.

##  How to build jctrace?
To build the project just run:
```
gradlew shadowJar
```

The built jar file it then located in `subprojects/jctrace-core/build/libs/jctrace-core-1.0.0-SNAPSHOT-all.jar

## Create a measurement
First you have to create a measurement of your running application.

Start your application with the following additional java vm arguments:
```
-javaagent:jctrace-core-1.0.0-SNAPSHOT-all.jar
-Xbootclasspath/a:jctrace-core-1.0.0-SNAPSHOT-all.jar
-Dde.turban.DeadLockTracer.blacklist=org.eclipse;com.google
-Dde.turban.DeadLockTracer.stacktracingClasses=<ClassName>;<SecondClassName>
```

You could blacklist whole packages including all sub packages with `-Dde.turban.DeadLockTracer.blacklist=`.

Packages are separated with an `;`.

#### Stacktrace
You could enable the collcetion of stacktraces for certain classes with `-Dde.turban.DeadLockTracer.stacktracingClasses`

Class names are separated with an `;`.
 

##### Full sample
Full Path sample:
```
-javaagent:/Path/To/Jar/File/jctrace-core-1.0.0-SNAPSHOT-all.jar
-Xbootclasspath/a:/Path/To/Jar/File/jctrace-core-1.0.0-SNAPSHOT-all.jar

-Dde.turban.DeadLockTracer.blacklist=org.eclipse;com.google;org.apache;java;sun;org.osgi;com.sun;org.codehaus.groovy;groovy

-Dde.turban.DeadLockTracer.stacktracingClasses=pkg.to.class.ClassNameA;pkg.to.other.class.ClassB

-Djava.util.logging.config.file=/Your/path/to/logging.properties
```


## Open UI to evaluate the Database

You could open the UI with:
```
gradlew runUI -PdbFile=Deadlock.db
```

Note: In the current version the UI will the show anything, if there is no potential deadlock.

## Create a report 
Or you could create an XML report from the Database, if you call:
```
gradlew runReportXml -PdbFile=Deadlock.db -PreportFile=DeadlockReport.xml 
```


## Further work to do (wish list)

* Improve the usability of the UI
 * Make the Load/Close buttons work correctly
 * Display text, if not potential deadlock was found
* Improve the usability of measurement creation with a wrapper which calls the java application
* Add instrumentation for volatile field reads and writes to detect data races
* Add instrumentation for Atomic* classes reads and writes to detect data races


