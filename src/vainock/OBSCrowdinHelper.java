package vainock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vainock.crowdin.CrowdinLogin;
import vainock.crowdin.CrowdinRequest;
import vainock.crowdin.CrowdinResponse;
import vainock.crowdin.HttpRequestMethod;

public class OBSCrowdinHelper {

    public static void main(String[] args) throws Exception {
	CrowdinRequest.setMaxRunningRequests(50);
	String rootPath = new File("").getAbsolutePath() + "/" + "OBSCrowdinHelper/";
	new File(rootPath).mkdirs();
	Scanner scanner = new Scanner(System.in);
	HashMap<String, ArrayList<String>> output = new HashMap<>();
	HashMap<Short, String> projectLanguages = new HashMap<>();

	// login
	println("OBSCrowdinHelper started!");
	boolean run = true;
	int read;
	File loginFile = new File(rootPath + "Login");
	if (loginFile.exists()) {
	    println("Try to use saved login information...");
	    FileReader loginInformationFr = new FileReader(loginFile);
	    StringBuilder loginInformationSb = new StringBuilder();
	    while ((read = loginInformationFr.read()) != -1)
		loginInformationSb.append(Character.valueOf((char) read));
	    loginInformationFr.close();
	    CrowdinLogin.login_csrf = loginInformationSb.toString().split(";")[0];
	    CrowdinLogin.login_cid = loginInformationSb.toString().split(";")[1];
	    if (checkIfValidUser()) {
		println("The saved login information is valid, do you want to continue with this account to skip the login? Valid inputs: Yes/No");
		boolean loginSkipRun = true;
		while (loginSkipRun) {
		    String input = scanner.nextLine();
		    if (input.equalsIgnoreCase("yes")) {
			loginSkipRun = false;
			run = false;
		    } else if (input.equalsIgnoreCase("no")) {
			loginSkipRun = false;
			loginFile.delete();
		    } else
			println("Please use a valid input: Yes/No");
		}
	    } else {
		println("The saved login information is unvalid, please login!");
		CrowdinLogin.login_csrf = null;
		CrowdinLogin.login_cid = null;
		loginFile.delete();
	    }
	} else
	    println("A login is required to continue!");

	while (run) {
	    println("----------");
	    println("Email:");
	    String loginEmail = scanner.nextLine();
	    println("Password:");
	    String loginPassword = scanner.nextLine();
	    println("Two Factor Authentication code: (Leave it out when you have no 2FA enabled.)");
	    String loginMfa = scanner.nextLine();
	    println("----------");
	    println("Logging in...");
	    if (loginMfa.isEmpty()) {
		CrowdinLogin.login(loginEmail, loginPassword);
	    } else
		CrowdinLogin.login(loginEmail, loginPassword, loginMfa);
	    if (CrowdinLogin.loginSuccessful()) {
		run = false;
		println("Login successful!");
	    } else
		println("The login was not successful, check your entered login information and try again!");
	}

	FileOutputStream loginFos = new FileOutputStream(loginFile);
	loginFos.write((CrowdinLogin.login_csrf + ";" + CrowdinLogin.login_cid).getBytes());
	loginFos.flush();
	loginFos.close();

	println("----------");
	println("To start collecting all the information, press Enter.");
	scanner.nextLine();
	println("Start fetching all datas:");

	println(" - clear OBSCrowdinHelper directory");

	for (File file : new File(rootPath).listFiles())
	    if (!file.getName().equals("Login"))
		deleteFile(file);

	println(" - request project members");

	// get project language names and ids
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
	CrowdinRequest.clearCrowdinResponses();

	// get project members
	int i = 1;
	for (short projectLanguageId : projectLanguages.keySet()) {
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
	    i++;
	}

	CrowdinRequest.waitForEveryRequest();

	// read and format project member's name
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
		    if (projectLanguageId == null)
			projectLanguageId = Short.valueOf(languagesArray.get(0).toString());
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

	println(" - generate Translators.txt");

	// save project members
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

	Writer out = new BufferedWriter(new OutputStreamWriter(
		new FileOutputStream(new File(rootPath + "Translators.txt")), StandardCharsets.UTF_8));
	out.append(resultSb.toString());
	out.flush();
	out.close();

	// build project
	System.out.print(" - check account permissions: Account has ");
	if (checkIfManager()) {
	    println("enough permissions.");
	    println(" - build project");
	    CrowdinRequest req2 = new CrowdinRequest();
	    req2.setUrl("backend/project_actions/export_project");
	    req2.setMethod(HttpRequestMethod.GET);
	    req2.addParam("project_id", "51028");
	    req2.send().getCrowdinResponse();

	    run = true;
	    while (run) {
		CrowdinRequest req = new CrowdinRequest();
		req.setUrl("backend/project_actions/check_export_status");
		req.setMethod(HttpRequestMethod.GET);
		req.addParam("project_id", "51028");
		JSONObject statusObj = (JSONObject) new JSONParser()
			.parse(req.send().getCrowdinResponse().getContent());
		if (Integer.valueOf(statusObj.get("progress").toString()) == 100) {
		    run = false;
		} else
		    Thread.sleep(1000);
	    }
	} else
	    println("not enough permissions, skip project build.");

	// download build
	println(" - download newest build");
	String buildFilePath = rootPath + "Translations.zip";
	Files.copy(new URL("https://crowdin.com/backend/download/project/obs-studio.zip").openStream(),
		new File(buildFilePath).toPath());

	// unzip build
	println(" - unzip build and delete empty files");
	ZipInputStream zipIn = new ZipInputStream(new FileInputStream(buildFilePath));
	ZipEntry entry = zipIn.getNextEntry();
	byte[] buffer = new byte[2048];
	while (entry != null) {
	    String filePath = rootPath + "Translations/" + entry.getName();
	    File file = new File(filePath);
	    if (entry.isDirectory())
		file.mkdirs();
	    if (!entry.isDirectory()) {
		FileOutputStream entryFos = new FileOutputStream(file);
		while ((read = zipIn.read(buffer, 0, buffer.length)) != -1)
		    entryFos.write(buffer, 0, read);
		entryFos.flush();
		entryFos.close();
		FileReader emptyFilesFr = new FileReader(filePath);
		StringBuilder emptyFilesSb = new StringBuilder();
		while ((read = emptyFilesFr.read()) != -1)
		    emptyFilesSb.append(Character.valueOf((char) read));
		emptyFilesFr.close();
		if (emptyFilesSb.toString().replaceAll("(\\r|\\n)", "").length() == 0)
		    file.delete();
	    }
	    zipIn.closeEntry();
	    entry = zipIn.getNextEntry();
	}
	zipIn.close();
	new File(buildFilePath).delete();

	println("Finished!");
	println("----------");
	println("Press Enter to close the program.");
	scanner.nextLine();
	scanner.close();
    }

    private static void println(String text) {
	System.out.println(text);
    }

    private static void deleteFile(File file) {
	if (file.isFile() && file.exists()) {
	    file.delete();
	} else {
	    for (File subFile : file.listFiles())
		deleteFile(subFile);
	    file.delete();
	}
    }

    private static boolean checkIfManager() {
	CrowdinRequest req = new CrowdinRequest();
	req.setUrl("backend/project_actions/check_export_status");
	req.setMethod(HttpRequestMethod.POST);
	req.addParam("project_id", "51028");
	try {
	    return (boolean) ((JSONObject) new JSONParser().parse(req.send().getCrowdinResponse().getContent()))
		    .get("success");
	} catch (ParseException e) {
	    return false;
	}
    }

    private static boolean checkIfValidUser() {
	CrowdinRequest req = new CrowdinRequest();
	req.setUrl("backend/tasks/get_tasks_progress");
	req.setMethod(HttpRequestMethod.GET);
	req.addParam("project_id", "51028");
	try {
	    return (boolean) ((JSONObject) new JSONParser().parse(req.send().getCrowdinResponse().getContent()))
		    .get("success");
	} catch (ParseException e) {
	    return false;
	}
    }
}