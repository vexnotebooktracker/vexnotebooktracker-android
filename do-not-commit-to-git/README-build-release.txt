Key-And-KeyStore-Spin-Up-Vex-Notebook-Tracker-2005#

PS F:\design\mobile\android\VexNotebookTracker> keytool -genkey -v -keystore vexnotebook-release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias vexnotebook
Enter keystore password:
Re-enter new password:
What is your first and last name?
  [Unknown]:  Yegna Bodepudi
What is the name of your organizational unit?
  [Unknown]:  App Development
What is the name of your organization?
  [Unknown]:  VexTeams.org
What is the name of your City or Locality?
  [Unknown]:  Dayton
What is the name of your State or Province?
  [Unknown]:  NJ
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=Yegna Bodepudi, OU=App Development, O=VexTeams.org, L=Dayton, ST=NJ, C=US correct?
  [no]:  yes

Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 10,000 days
        for: CN=Yegna Bodepudi, OU=App Development, O=VexTeams.org, L=Dayton, ST=NJ, C=US
[Storing vexnotebook-release-keystore.jks]
dir .\vexnotebook-release-keystore.jks
    Directory: F:\design\mobile\android\VexNotebookTracker
Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----         7/25/2025  10:48 PM           2755 vexnotebook-release-keystore.jks
