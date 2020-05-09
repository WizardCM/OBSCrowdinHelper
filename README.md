## OBSCrowdinHelper
OBSCrowdinHelper is a simple Java tool to fetch the newest translations and translators from the [OBS Studio Crowdin project](https://crowdin.com/project/obs-studio).

### Requirements
- Java SE 1.6 or higher
- Windows (should also work on macOS but this wasn't tested)
- A Crowdin Account
<details><summary>Why does the program need my account credentials?</summary>

The reason for this is quite simple:
The program tries to view the [OBS Studio reports page](https://crowdin.com/project/obs-studio/reports) to retrieve its data but with no login it will be redirected to the login page. Because of that reason, OBSCrowdinHelper needs a valid Crowdin Account to be able to request the project translators.
</details>
