package vainock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import vainock.crowdin.CrowdinLogin;
import vainock.crowdin.CrowdinRequest;
import vainock.crowdin.CrowdinResponse;
import vainock.crowdin.HttpRequestMethod;

public class OBSCrowdinHelper {

    public static void main(String[] args) throws Exception {
	CrowdinRequest.setMaxRunningRequests(50);
	String resultingFileName = "OBS Studio Translators.txt";
	String rootPath = new File("").getAbsolutePath() + "\\";
	Scanner scanner = new Scanner(System.in);
	HashMap<String, ArrayList<String>> output = new HashMap<>();
	HashMap<Short, String> projectLanguages = new HashMap<>();

	System.out.println("OBSCrowdinHelper started! - A login is required to continue!");

	boolean run = true;
	while (run) {
	    System.out.println("----------");
	    System.out.println("Email:");
	    String loginEmail = scanner.nextLine();
	    System.out.println("Password:");
	    String loginPassword = scanner.nextLine();
	    System.out.println("Two Factor Authentication code: (Leave it out when you have no 2FA enabled.)");
	    String loginMfa = scanner.nextLine();
	    System.out.println("----------");
	    System.out.println("Logging in...");
	    if (loginMfa.isEmpty()) {
		CrowdinLogin.login(loginEmail, loginPassword);
	    } else
		CrowdinLogin.login(loginEmail, loginPassword, loginMfa);
	    if (CrowdinLogin.loginSuccessful()) {
		run = false;
	    } else
		System.out.println("The login was not successful, check your entered login information and try again!");
	}

	System.out.println("Login successful!");
	System.out.println("----------");
	System.out.println("To start collecting all the information, press Enter.");
	scanner.nextLine();

	// Get all project language names and ids.
	CrowdinRequest req1 = new CrowdinRequest();
	req1.setUrl("backend/translate/get_editor_data");
	req1.setMethod(HttpRequestMethod.GET);
	req1.addParam("editor_mode", "translate");
	req1.addParam("project_identifier", "obs-studio");
	req1.addParam("file_id", "all");
	req1.addParam("languages", "en-de");
	req1.addParam("original_url", "https://crowdin.com/translate/obs-studio/all/en-de");
	CrowdinResponse res1 = req1.send().getCrowdinResponse();

	JSONObject editorObj = (JSONObject) new JSONParser().parse(res1.getContent());
	JSONObject dataObj = (JSONObject) editorObj.get("data");
	JSONObject initEditorObj = (JSONObject) dataObj.get("init_editor");
	JSONObject projectObj = (JSONObject) initEditorObj.get("project");
	JSONArray targetLanguagesArray = (JSONArray) projectObj.get("target_languages");

	for (Object language : targetLanguagesArray) {
	    JSONObject languageObj = (JSONObject) language;
	    projectLanguages.put(Short.valueOf(languageObj.get("id").toString()), languageObj.get("name").toString());
	}
	System.out.println("Tracked " + projectLanguages.size() + " project languages.");
	CrowdinRequest.clearCrowdinResponses();
	System.out.println("Start requesting all project members...");

	// Get all project members.
	int i = 1;
	for (Short projectLanguageId : projectLanguages.keySet()) {
	    CrowdinRequest req = new CrowdinRequest();
	    req.setMethod(HttpRequestMethod.POST);
	    req.setUrl("backend/user_reports/get_top_members");
	    req.addParam("project_id", "51028");
	    req.addParam("report_mode", "words");
	    req.addParam("language_id", String.valueOf(projectLanguageId));
	    req.addParam("date_from", "2014-07-07");
	    req.addParam("date_to", "2030-01-01");
	    req.addParam("page", "1");
	    req.addParam("sortname", "translated");
	    req.addParam("sortorder", "desc");
	    req.addParam("rp", "50");
	    req.addParam("filter", "");
	    req.addParam("request", String.valueOf(i));
	    req.send();

	    System.out.println("Language: " + i + "/" + projectLanguages.size());
	    i++;
	}

	CrowdinRequest.waitForEveryRequest();
	System.out.println("Sent all requests, start generating '" + resultingFileName + "' in '" + rootPath + "'...");

	for (CrowdinResponse res : CrowdinRequest.getCrowdinResponses()) {
	    ArrayList<String> languageUsers = new ArrayList<>();

	    JSONObject resObj = (JSONObject) new JSONParser().parse(res.getContent());
	    Short projectLanguageId = null;
	    JSONArray rowsArray = (JSONArray) resObj.get("rows");
	    for (Object user : rowsArray) {
		JSONObject rowObj = (JSONObject) user;
		JSONObject userObj = (JSONObject) rowObj.get("_user");
		JSONArray languagesArray = (JSONArray) rowObj.get("languages");
		String userName = (String) userObj.get("name");
		String userLogin = (String) userObj.get("login");
		if (!languagesArray.isEmpty()) {
		    if (projectLanguageId == null) {
			projectLanguageId = Short.valueOf(languagesArray.get(0).toString());
		    }
		    String userFullName;
		    if (userName != null) {
			if (userName.isEmpty()) {
			    userFullName = userLogin;
			} else
			    userFullName = userName + " (" + userLogin + ")";
		    } else
			userFullName = userLogin;
		    languageUsers.add(userFullName);
		}
	    }
	    if (projectLanguageId != null)
		output.put(projectLanguages.get(projectLanguageId), languageUsers);
	}

	// Save all project members in the Authors.txt file.
	ArrayList<String> translatedLangs = new ArrayList<>();

	for (String keySetObj : output.keySet())
	    translatedLangs.add(keySetObj);

	Collections.sort(translatedLangs);

	StringBuilder resultSb = new StringBuilder();
	resultSb.append("Translators:\n");
	for (String lang : translatedLangs) {
	    resultSb.append(" " + lang + ":\n");
	    for (String userPerLang : output.get(lang))
		resultSb.append("  - " + userPerLang + "\n");
	}
	resultSb.deleteCharAt(resultSb.length() - 1);

	Writer out = new BufferedWriter(
		new OutputStreamWriter(new FileOutputStream(new File(rootPath + resultingFileName)), "UTF-8"));
	out.append(resultSb.toString());
	out.flush();
	out.close();

	System.out.println("Finished!");
	System.out.println("----------");
	System.out.println("Press Enter to close the program.");
	scanner.nextLine();
	scanner.close();
    }
}