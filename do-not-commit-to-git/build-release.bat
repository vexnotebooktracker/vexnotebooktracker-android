call .\gradlew --stop
adb shell am kill org.vexteams.vexnotebooktracker
adb shell pm clear org.vexteams.vexnotebooktracker
.\gradlew clean
Remove-Item -Path .gradle -Recurse -Force
Remove-Item -Path build -Recurse -Force
Remove-Item -Path app\build -Recurse -Force
adb uninstall org.vexteams.vexnotebooktracker
.\gradlew assembleRelease
adb install "app/build/outputs/apk/release/app-release.apk"
