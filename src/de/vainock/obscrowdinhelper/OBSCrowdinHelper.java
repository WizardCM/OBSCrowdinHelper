package de.vainock.obscrowdinhelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
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

import de.vainock.obscrowdinhelper.crowdin.CrowdinCookieJar;
import de.vainock.obscrowdinhelper.crowdin.CrowdinLogin;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequest;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequestMethod;
import de.vainock.obscrowdinhelper.crowdin.CrowdinResponse;
import okhttp3.Cookie;

public class OBSCrowdinHelper {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		try {
			File root = new File(new File("").getAbsolutePath());
			if (args.length == 0) {
				File batchFile = new File(System.getenv("TEMP") + File.separator + "OBSCrowdinHelper.bat");
				String jarPath = ClassLoader.getSystemClassLoader()
						.getResource(OBSCrowdinHelper.class.getResource(OBSCrowdinHelper.class.getSimpleName() + ".class").getFile()).getFile();
				Files.write(batchFile.toPath(),
						("@echo off\ntitle OBSCrowdinHelper\ncls\njava -jar \"" + URLDecoder.decode(jarPath.substring(6, jarPath.lastIndexOf('!')), "UTF8") + "\" prompt\nexit")
								.getBytes());
				Runtime.getRuntime().exec("cmd /c start " + batchFile);
				System.exit(0);
			}

			// login
			out("OBSCrowdinHelper started!");
			CrowdinCookieJar cj = CrowdinCookieJar.getInstance();
			boolean run = true;
			int read;
			File loginFile = new File(root, "Login");
			if (loginFile.exists()) {
				out("Try to use saved login information...");
				FileReader loginInformationFr = null;
				loginInformationFr = new FileReader(loginFile);
				StringBuilder loginInformationSb = new StringBuilder();
				while ((read = loginInformationFr.read()) != -1)
					loginInformationSb.append(Character.valueOf((char) read));
				List<Cookie> cookies = new ArrayList<>();
				for (String cookieRaw : loginInformationSb.toString().split("\n")) {
					String[] cookie = cookieRaw.split(";");
					cookies.add(new Cookie.Builder().name(cookie[0]).value(cookie[1]).domain(cookie[2]).path("/").build());
				}
				cj.setCookies(cookies);
				loginInformationFr.close();
				if (CrowdinLogin.loginSuccessful()) {
					out("The saved login information is valid, do you want to continue with this account to skip the login process? Valid inputs: Yes/No");
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
							out("Please enter a valid input: Yes/No");
					}
				} else {
					out("The saved login information is invalid, please login!");
					loginFile.delete();
				}
			} else
				out("A login is required to continue!");

			while (run) {
				out("----------");
				out("Email:");
				String loginEmail = scanner.nextLine();
				out("Password:");
				String loginPassword = scanner.nextLine();
				out("Two Factor Authentication code: (Leave it out when you have no 2FA enabled.)");
				String loginMfa = scanner.nextLine();
				out("----------");
				out("Logging in...");
				CrowdinLogin.login(loginEmail, loginPassword, loginMfa);
				if (CrowdinLogin.loginSuccessful()) {
					run = false;
					out("Login successful!");
				} else
					out("The login was not successful, check your entered login information and try again!");
			}
			FileOutputStream loginFos = new FileOutputStream(loginFile);
			StringBuilder cookiesSB = new StringBuilder();
			for (Cookie cookie : cj.getCookies())
				cookiesSB.append(cookie.name() + ";" + cookie.value() + ";" + cookie.domain() + "\n");
			loginFos.write(cookiesSB.toString().getBytes());
			loginFos.flush();
			loginFos.close();

			out("----------");
			out("Press Enter to start collecting the data.");
			scanner.nextLine();
			out(" - removing Translators.txt, Translations.zip and Translations files");
			for (File file : root.listFiles())
				if (file.getName().equals("Translators.txt") || file.getName().equals("Translations") || file.getName().equals("Translations.zip"))
					deleteFile(file);
			out(" - requesting project members");
			// get project language names and ids
			CrowdinRequest req1 = new CrowdinRequest();
			req1.setUrl("crowdin.com/backend/translate/get_editor_data");
			req1.setMethod(CrowdinRequestMethod.GET);
			req1.setParam("editor_mode", "translate");
			req1.setParam("project_identifier", "obs-studio");
			req1.setParam("file_id", "all");
			req1.setParam("languages", "en-de");
			req1.setParam("original_url", "https://crowdin.com/translate/obs-studio/all/en-de");
			CrowdinResponse res1 = req1.send();

			Map<Short, String> projectLanguages = new HashMap<>();
			for (Object language : (JSONArray) ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) new JSONParser().parse(res1.getContent())).get("data"))
					.get("init_editor")).get("project")).get("target_languages")) {
				JSONObject languageObj = (JSONObject) language;
				projectLanguages.put(Short.valueOf(languageObj.get("id").toString()), languageObj.get("name").toString());
			}

			// get project members
			int i = 1;
			List<CrowdinRequest> requests = new ArrayList<>();
			CrowdinRequest.setMaxRunningRequests(50);
			for (short projectLanguageId : projectLanguages.keySet()) {
				CrowdinRequest req = new CrowdinRequest();
				req.setMethod(CrowdinRequestMethod.POST);
				req.setUrl("crowdin.com/backend/user_reports/get_top_members");
				req.setFormEntry("project_id", "51028");
				req.setFormEntry("report_mode", "words");
				req.setFormEntry("language_id", String.valueOf(projectLanguageId));
				req.setFormEntry("date_from", "2014-07-07");
				req.setFormEntry("date_to", "2030-01-01");
				req.setFormEntry("page", "1");
				req.setFormEntry("sortname", "translated");
				req.setFormEntry("sortorder", "desc");
				req.setFormEntry("rp", "50");
				req.setFormEntry("filter", "");
				req.setFormEntry("request", String.valueOf(i));
				i++;
				requests.add(req);
			}

			// read and format project member names
			Map<String, List<String>> output = new HashMap<>();
			for (CrowdinResponse res : CrowdinRequest.send(requests)) {
				List<String> languageUsers = new ArrayList<>();
				Short projectLanguageId = null;
				for (Object user : (JSONArray) ((JSONObject) new JSONParser().parse(res.getContent())).get("rows")) {
					JSONObject rowObj = (JSONObject) user;
					JSONObject userObj = (JSONObject) rowObj.get("_user");
					JSONArray languagesArray = (JSONArray) rowObj.get("languages");
					String userName = (String) userObj.get("name");
					String userLogin = (String) userObj.get("login");
					if (userLogin.equals("REMOVED_USER"))
						continue;
					if (languagesArray.isEmpty())
						continue;
					if (projectLanguageId == null)
						projectLanguageId = Short.valueOf(languagesArray.get(0).toString());
					String userFullName;
					if (userName != null)
						if (userName.isEmpty())
							userFullName = userLogin;
						else
							userFullName = userName + " (" + userLogin + ")";
					else
						userFullName = userLogin;
					languageUsers.add(userFullName);
				}
				if (projectLanguageId != null)
					output.put(projectLanguages.get(projectLanguageId), languageUsers);
			}

			out(" - generating Translators.txt");

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
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(root, "Translators.txt")), StandardCharsets.UTF_8));
			out.append(resultSb.toString());
			out.flush();
			out.close();
			// build project
			System.out.print(" - checking account permissions: ");
			if (isUserManager()) {
				out("Account has enough permissions.");
				out(" - building OBS Studio project");
				CrowdinRequest req2 = new CrowdinRequest();
				req2.setUrl("crowdin.com/backend/project_actions/export_project");
				req2.setMethod(CrowdinRequestMethod.GET);
				req2.setParam("project_id", "51028");
				req2.send();

				run = true;
				while (run) {
					CrowdinRequest req = new CrowdinRequest();
					req.setUrl("crowdin.com/backend/project_actions/check_export_status");
					req.setMethod(CrowdinRequestMethod.GET);
					req.setParam("project_id", "51028");
					JSONObject statusObj = (JSONObject) new JSONParser().parse(req.send().getContent());
					if (Integer.valueOf(statusObj.get("progress").toString()) == 100) {
						run = false;
					} else
						Thread.sleep(1000);
				}
			} else
				out("Account has not enough permissions, skip project build");
			// download build
			out(" - downloading newest build");
			File buildFile = new File(root, "Translations.zip");
			Files.copy(new URL("https://crowdin.com/backend/download/project/obs-studio.zip").openStream(), buildFile.toPath());
			// unzip build
			out(" - unzipping build and deleting empty files");
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(buildFile));
			ZipEntry entry = zipIn.getNextEntry();
			byte[] buffer = new byte[2048];
			while (entry != null) {
				File file = new File(root, "Translations" + File.separator + entry.getName());
				if (entry.isDirectory())
					file.mkdirs();
				if (!entry.isDirectory()) {
					FileOutputStream entryFos = new FileOutputStream(file);
					while ((read = zipIn.read(buffer, 0, buffer.length)) != -1)
						entryFos.write(buffer, 0, read);
					entryFos.flush();
					entryFos.close();
					FileReader emptyFilesFr = new FileReader(file);
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
			buildFile.delete();

			out("Finished!");
			out("----------");
			out("Press Enter to close the program.");
		} catch (Exception e) {
			out("----- An error occured: -----");
			out("Please close the program and try again. If this doesn't help, contact the developer: contact.vainock@gmail.com");
			out("Please also provide the following error message which will help to identify and fix the bug:");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			out(sw.toString());
		}
		scanner.nextLine();
		System.exit(0);
		scanner.close();
	}

	private static void out(String text) {
		System.out.println(text);
	}

	private static void deleteFile(File file) {
		if (file.isFile()) {
			if (file.exists())
				file.delete();
		} else {
			for (File subFile : file.listFiles())
				deleteFile(subFile);
			if (file.exists())
				file.delete();
		}
	}

	private static boolean isUserManager() {
		CrowdinRequest req = new CrowdinRequest();
		req.setUrl("crowdin.com/backend/project_actions/check_export_status");
		req.setMethod(CrowdinRequestMethod.POST);
		req.setParam("project_id", "51028");
		try {
			return (boolean) ((JSONObject) new JSONParser().parse(req.send().getContent())).get("success");
		} catch (Exception e) {
			return false;
		}
	}
}