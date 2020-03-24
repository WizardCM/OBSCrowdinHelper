package de.vainock.obscrowdinhelper;

import java.io.BufferedReader;
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
import java.nio.file.StandardCopyOption;
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
			// open jar with batch console
			File root = new File(new File("").getAbsolutePath());
			if (args.length == 0) {
				String jarPath = ClassLoader.getSystemClassLoader()
						.getResource(OBSCrowdinHelper.class.getResource(OBSCrowdinHelper.class.getSimpleName() + ".class").getFile()).getFile();
				Runtime.getRuntime().exec("cmd /c start " + Files.write(File.createTempFile("OBSCrowdinHelper", ".bat").toPath(),
						("@echo off\ntitle OBSCrowdinHelper\ncls\njava -jar \"" + URLDecoder.decode(jarPath.substring(6, jarPath.lastIndexOf('!')), "UTF8") + "\" prompt\nexit")
								.getBytes()));
				System.exit(0);
			}

			out("OBSCrowdinHelper started!\nPlease follow the instructions:\n");
			File conFile = new File(root, "Contributors.txt");
			conFile.createNewFile();
			out("Paste all the git contributors into the Contributors.txt file and press Enter after saving the file.");
			while (true) {
				scanner.nextLine();
				if (conFile.length() > 0)
					break;
				else
					out("The file is still empty, please try again!");
			}
			FileReader conFr = new FileReader(conFile);
			BufferedReader conBr = new BufferedReader(conFr);
			StringBuilder conSb = new StringBuilder();
			String line;
			while ((line = conBr.readLine()) != null) {
				while (true)
					if (line.startsWith(" ") || line.startsWith("-"))
						line = line.substring(1);
					else
						break;
				conSb.append(" - ");
				conSb.append(line);
				conSb.append('\n');
			}
			conFr.close();
			conBr.close();
			conFile.delete();

			clearScreen();

			// login
			out("A Crowdin login is now required to continue.");
			CrowdinCookieJar cj = CrowdinCookieJar.getInstance();
			int read;
			File loginFile = new File(root, "Login");
			boolean loginRequired = true;
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
					while (true) {
						String input = scanner.nextLine();
						if (input.equalsIgnoreCase("yes")) {
							loginRequired = false;
							break;
						} else if (input.equalsIgnoreCase("no")) {
							loginFile.delete();
							break;
						} else
							out("Please enter a valid input: Yes/No");
					}
				} else {
					out("The saved login information is invalid, please login!");
					loginFile.delete();
				}
			}

			if (loginRequired)
				while (true) {
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
					if (CrowdinLogin.loginSuccessful())
						break;
					else
						out("The login was not successful, check your entered login information and try again!");
				}
			FileOutputStream loginFos = new FileOutputStream(loginFile);
			StringBuilder cookiesSB = new StringBuilder();
			for (Cookie cookie : cj.getCookies())
				cookiesSB.append(cookie.name() + ";" + cookie.value() + ";" + cookie.domain() + "\n");
			loginFos.write(cookiesSB.toString().getBytes());
			loginFos.flush();
			loginFos.close();

			clearScreen();

			out("Start generating the project structure.");
			File repoRoot = new File(root, "OBS Studio Repository Structure");
			if (repoRoot.exists()) {
				out(" - clearing " + repoRoot.getName() + " folder");
				for (File file : repoRoot.listFiles())
					deleteFile(file);
			} else
				repoRoot.mkdir();
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

			out(" - generating AUTHORS file");

			List<String> translatedLangs = new ArrayList<>();
			for (String keySetObj : output.keySet())
				translatedLangs.add(keySetObj);
			Collections.sort(translatedLangs);
			StringBuilder authSb = new StringBuilder();
			authSb.append("Original Author: Hugh Bailey (\"Jim\")\n\nContributors are sorted by their amount of commits / translated words.\n\nCommitters:\n");
			authSb.append(conSb.toString());
			authSb.append("\nTranslators:\n");
			for (String lang : translatedLangs) {
				authSb.append(' ');
				authSb.append(lang);
				authSb.append(":\n");
				for (String userPerLang : output.get(lang)) {
					authSb.append("  - ");
					authSb.append(userPerLang);
					authSb.append('\n');
				}
			}
			authSb.deleteCharAt(authSb.length() - 1);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(repoRoot, "AUTHORS")), StandardCharsets.UTF_8));
			out.append(authSb.toString());
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

				while (true) {
					CrowdinRequest req = new CrowdinRequest();
					req.setUrl("crowdin.com/backend/project_actions/check_export_status");
					req.setMethod(CrowdinRequestMethod.GET);
					req.setParam("project_id", "51028");
					JSONObject statusObj = (JSONObject) new JSONParser().parse(req.send().getContent());
					if (Integer.valueOf(statusObj.get("progress").toString()) == 100) {
						break;
					} else
						Thread.sleep(1000);
				}
			} else
				out("Account has not enough permissions, skipping project build");
			out(" - downloading newest build");
			File buildFile = new File(root, "Translations.zip");
			Files.copy(new URL("https://crowdin.com/backend/download/project/obs-studio.zip").openStream(), buildFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			out(" - unzipping build and deleting empty files");
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(buildFile));
			ZipEntry entry = zipIn.getNextEntry();
			byte[] buffer = new byte[2048];
			while (entry != null) {
				File file = new File(repoRoot, entry.getName());
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
			File webTrans = new File(root, "Website Translations");
			if (webTrans.exists()) {
				out(" - clearing " + webTrans.getName() + " folder");
				for (File file : webTrans.listFiles())
					deleteFile(file);
			}
			Files.move(new File(repoRoot, "Website").toPath(), webTrans.toPath(), StandardCopyOption.REPLACE_EXISTING);

			clearScreen();

			out("Finished!\nYou can now simply copy-paste all files from the " + repoRoot.getName()
					+ " folder to the OBS Studio repository folder and push the changes.\nThe website translations can be found next to this .jar file.");
		} catch (Exception e) {
			out("----- An error occured: -----\nPlease close the program and try again. If this doesn't help, contact the developer: contact.vainock@gmail.com\nPlease also provide the following error message which will help to identify and fix the bug:");
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

	private static void clearScreen() throws Exception {
		new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
	}
}