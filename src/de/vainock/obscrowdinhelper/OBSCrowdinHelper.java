package de.vainock.obscrowdinhelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.vainock.obscrowdinhelper.crowdin.CrowdinCookieJar;
import de.vainock.obscrowdinhelper.crowdin.CrowdinLogin;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequest;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequestMethod;
import de.vainock.obscrowdinhelper.crowdin.CrowdinResponse;

public class OBSCrowdinHelper {
	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		// login
		println("OBSCrowdinHelper started!");
		CrowdinCookieJar cj = CrowdinCookieJar.getInstance();
		boolean run = true;
		int read;
		String rootPath = new File("").getAbsolutePath() + File.separator;
		new File(rootPath).mkdirs();
		File loginFile = new File(rootPath + "Login");
		if (loginFile.exists()) {
			println("Try to use saved login information...");
			FileReader loginInformationFr = null;
			try {
				loginInformationFr = new FileReader(loginFile);
			} catch (FileNotFoundException e) {
				cancel("The file '" + loginFile.getAbsolutePath() + "' wasn't found.", 1, e);
			}
			StringBuilder loginInformationSb = new StringBuilder();
			try {
				while ((read = loginInformationFr.read()) != -1)
					loginInformationSb.append(Character.valueOf((char) read));
				cj.setCookies(loginInformationSb.toString());
				loginInformationFr.close();
			} catch (IOException e) {
				cancel("The file '" + loginFile + "' couldn't be read.", 2, e);
			}
			if (checkIfValidUser()) {
				println(
						"The saved login information is valid, do you want to continue with this account to skip the login? Valid inputs: Yes/No");
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
				println("The saved login information is invalid, please login!");
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
			CrowdinLogin.login(loginEmail, loginPassword, loginMfa);
			if (CrowdinLogin.loginSuccessful()) {
				run = false;
				println("Login successful!");
			} else
				println("The login was not successful, check your entered login information and try again!");
		}

		try {
			FileOutputStream loginFos = new FileOutputStream(loginFile);
			loginFos.write(cj.getCookiesRaw().getBytes());
			loginFos.flush();
			loginFos.close();
		} catch (IOException e) {
			cancel("The file '" + loginFile.getAbsolutePath() + "' couldn't be saved.", 3, e);
		}

		println("----------");
		println("To start collecting all the information, press Enter.");
		scanner.nextLine();
		println("Start fetching all datas:");

		println(" - clear OBSCrowdinHelper directory");

		for (File file : new File(rootPath).listFiles())
			if (file.getName().equals("Translators.txt") || file.getName().equals("Translations")
					|| file.getName().equals("Translations.zip"))
				deleteFile(file);

		println(" - request project members");

		// get project language names and ids
		CrowdinRequest req1 = new CrowdinRequest();
		req1.setUrl("crowdin.com/backend/translate/get_editor_data");
		req1.setMethod(CrowdinRequestMethod.GET);
		req1.addParam("editor_mode", "translate");
		req1.addParam("project_identifier", "obs-studio");
		req1.addParam("file_id", "all");
		req1.addParam("languages", "en-de");
		req1.addParam("original_url", "https://crowdin.com/translate/obs-studio/all/en-de");
		CrowdinResponse res1 = req1.send();

		Map<Short, String> projectLanguages = new HashMap<>();
		try {
			for (Object language : (JSONArray) ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) new JSONParser()
					.parse(res1.getContent())).get("data")).get("init_editor")).get("project")).get("target_languages")) {
				JSONObject languageObj = (JSONObject) language;
				projectLanguages.put(Short.valueOf(languageObj.get("id").toString()), languageObj.get("name").toString());
			}
		} catch (ParseException e) {
			cancel("Received data couldn't be parsed.", 4, e);
		}

		// get project members
		int i = 1;
		List<CrowdinRequest> requests = new ArrayList<>();
		for (short projectLanguageId : projectLanguages.keySet()) {
			CrowdinRequest req = new CrowdinRequest();
			req.setMethod(CrowdinRequestMethod.POST);
			req.setUrl("crowdin.com/backend/user_reports/get_top_members");
			req.addFormEntry("project_id", "51028");
			req.addFormEntry("report_mode", "words");
			req.addFormEntry("language_id", String.valueOf(projectLanguageId));
			req.addFormEntry("date_from", "2014-07-07");
			req.addFormEntry("date_to", "2030-01-01");
			req.addFormEntry("page", "1");
			req.addFormEntry("sortname", "translated");
			req.addFormEntry("sortorder", "desc");
			req.addFormEntry("rp", "50");
			req.addFormEntry("filter", "");
			req.addFormEntry("request", String.valueOf(i));
			i++;
			requests.add(req);
		}

		// read and format project member's name
		Map<String, List<String>> output = new HashMap<>();
		for (CrowdinResponse res : CrowdinRequest.send(requests)) {
			ArrayList<String> languageUsers = new ArrayList<>();
			Short projectLanguageId = null;
			try {
				for (Object user : (JSONArray) ((JSONObject) new JSONParser().parse(res.getContent())).get("rows")) {
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
			} catch (ParseException e) {
				cancel("Received data couldn't be parsed.", 5, e);
			}
			if (projectLanguageId != null)
				output.put(projectLanguages.get(projectLanguageId), languageUsers);
		}

		println(" - generate Translators.txt");

		// save project members
		List<String> translatedLangs = new ArrayList<>();

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

		File translatorsFile = new File(rootPath + "Translators.txt");
		try {
			Writer out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(translatorsFile), StandardCharsets.UTF_8));
			out.append(resultSb.toString());
			out.flush();
			out.close();
		} catch (IOException e) {
			cancel("The file " + translatorsFile.getAbsolutePath() + " couldn't be saved.", 6, e);
		}

		// build project
		System.out.print(" - check account permissions: ");
		if (checkIfManager()) {
			println("Account has enough permissions.");
			println(" - build project");
			CrowdinRequest req2 = new CrowdinRequest();
			req2.setUrl("crowdin.com/backend/project_actions/export_project");
			req2.setMethod(CrowdinRequestMethod.GET);
			req2.addParam("project_id", "51028");
			req2.send();

			run = true;
			while (run) {
				CrowdinRequest req = new CrowdinRequest();
				req.setUrl("crowdin.com/backend/project_actions/check_export_status");
				req.setMethod(CrowdinRequestMethod.GET);
				req.addParam("project_id", "51028");
				JSONObject statusObj = null;
				try {
					statusObj = (JSONObject) new JSONParser().parse(req.send().getContent());
				} catch (ParseException e) {
					cancel("Received data couldn't be parsed.", 7, e);
				}
				if (Integer.valueOf(statusObj.get("progress").toString()) == 100) {
					run = false;
				} else
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						cancel("There was a problem with a thread.", 8, e);
					}
			}
		} else
			println("Account has not enough permissions, skip project build.");

		// download build
		println(" - download newest build");
		File buildFile = new File(rootPath + "Translations.zip");
		try {
			Files.copy(new URL("https://crowdin.com/backend/download/project/obs-studio.zip").openStream(),
					buildFile.toPath());
		} catch (IOException e) {
			cancel("The build file couldn't be downloaded.", 9, e);
		}

		// unzip build
		println(" - unzip build and delete empty files");
		ZipInputStream zipIn = null;
		try {
			zipIn = new ZipInputStream(new FileInputStream(buildFile));
		} catch (FileNotFoundException e) {
			cancel("The file '" + buildFile.getAbsolutePath() + "' wasn't found.", 10, e);
		}
		ZipEntry entry = null;
		try {
			entry = zipIn.getNextEntry();
			byte[] buffer = new byte[2048];
			while (entry != null) {
				String filePath = rootPath + "Translations" + File.separator + entry.getName();
				File file = new File(filePath);
				if (entry.isDirectory())
					file.mkdirs();
				if (!entry.isDirectory()) {
					try {
						FileOutputStream entryFos = new FileOutputStream(file);
						while ((read = zipIn.read(buffer, 0, buffer.length)) != -1)
							entryFos.write(buffer, 0, read);
						entryFos.flush();
						entryFos.close();
					} catch (IOException e) {
						cancel("The file " + file.getAbsolutePath() + " couldn't be saved.", 11, e);
					}
					try {
						FileReader emptyFilesFr = new FileReader(filePath);
						StringBuilder emptyFilesSb = new StringBuilder();
						while ((read = emptyFilesFr.read()) != -1)
							emptyFilesSb.append(Character.valueOf((char) read));
						emptyFilesFr.close();
						if (emptyFilesSb.toString().replaceAll("(\\r|\\n)", "").length() == 0)
							file.delete();
					} catch (IOException e) {
						cancel("The file " + file.getAbsolutePath() + " couldn't be read.", 12, e);
					}
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
		} catch (IOException e) {
			cancel("There was an error unzipping the build.", 13, e);
		}
		buildFile.delete();

		println("Finished!");
		println("----------");
		println("Press Enter to close the program.");
		scanner.nextLine();
	}

	private static void println(String text) {
		System.out.println(text);
	}

	private static void cancel(String reason, int code, Exception e) {
		println("----- An error occured: -----");
		println(reason);
		println("----------");
		println(
				"Please close the program and try again. If this doesn't help, contact the developer: contact.vainock@gmail.com");
		println("Please also provide the error code " + code
				+ " and the following error message which will help to identify and fix the bug:");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		println(sw.toString());
		scanner.nextLine();
		System.exit(0);
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
		req.setUrl("crowdin.com/backend/project_actions/check_export_status");
		req.setMethod(CrowdinRequestMethod.POST);
		req.addParam("project_id", "51028");
		try {
			return (boolean) ((JSONObject) new JSONParser().parse(req.send().getContent())).get("success");
		} catch (ParseException e) {
			return false;
		}
	}

	private static boolean checkIfValidUser() {
		CrowdinRequest req = new CrowdinRequest();
		req.setUrl("crowdin.com/backend/tasks/get_tasks_progress");
		req.setMethod(CrowdinRequestMethod.GET);
		req.addParam("project_id", "51028");
		try {
			return (boolean) ((JSONObject) new JSONParser().parse(req.send().getContent())).get("success");
		} catch (ParseException e) {
			return false;
		}
	}
}