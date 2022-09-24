# Service poller

# Building
We recommend using IntelliJ as it's what we use day to day at the KRY office.
In intelliJ, choose
```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```

You can also run gradle directly from the command line:
```
./gradlew clean run
```

# Notes

- Project was developed and tested in MacOS Catalina, using Java 11, Gradle 6.x, IntelliJ Idea 2019.3 and latest Chrome Browser.
- To run gradle command `build.gradle` script was modified because of different version.
- Before first run it is important to run `DBMigration.main()` in order to create database structure.


