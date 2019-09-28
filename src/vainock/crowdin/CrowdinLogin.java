package vainock.crowdin;

import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CrowdinLogin {

    public static String login_cid;
    public static String login_csrf;

    private CrowdinLogin() {

    }

    public static void login() {
	login_csrf = getRandomString(10);
	login_cid = getRandomString(26);
    }

    public static void login(String login, String password) {
	String csrf = getRandomString(10);
	CrowdinRequest req = new CrowdinRequest();
	req.setProperty("Cookie", "csrf_token=" + csrf);
	req.setProperty("x-csrf-token", csrf);
	req.setUrl("backend/login/submit");
	req.setMethod(HttpRequestMethod.POST);
	req.addParam("login", login);
	req.addParam("password", password);
	CrowdinResponse res = req.send().getCrowdinResponse();
	if (loginSuccessful(res.getContent())) {
	    login_cid = res.getCookie("cid");
	    login_csrf = csrf;
	}
	CrowdinRequest.clearCrowdinResponses();
    }

    public static void login(String login, String password, String mfaCode) {
	String csrf = getRandomString(10);
	CrowdinRequest req1 = new CrowdinRequest();
	req1.setProperty("Cookie", "csrf_token=" + csrf);
	req1.setProperty("x-csrf-token", csrf);
	req1.setUrl("backend/login/submit");
	req1.setMethod(HttpRequestMethod.POST);
	req1.addParam("login", login);
	req1.addParam("password", password);
	JSONObject obj = null;
	try {
	    obj = (JSONObject) new JSONParser().parse(req1.send().getCrowdinResponse().getContent());
	} catch (ParseException e) {
	    e.printStackTrace();
	}

	CrowdinRequest req2 = new CrowdinRequest();
	req2.setProperty("Cookie", "csrf_token=" + csrf);
	req2.setProperty("x-csrf-token", csrf);
	req2.setMethod(HttpRequestMethod.POST);
	req2.setUrl("backend/login/submit");
	req2.addParam("login", login);
	req2.addParam("password", password);
	req2.addParam("mfa_code", mfaCode);
	req2.addParam("mfa_hash", obj.get("mfa_hash").toString());
	CrowdinResponse res2 = req2.send().getCrowdinResponse();
	if (loginSuccessful(res2.getContent())) {
	    login_cid = res2.getCookie("cid");
	    login_csrf = csrf;
	}
	CrowdinRequest.clearCrowdinResponses();
    }

    public static boolean loginSuccessful() {
	return !(login_cid == null && login_csrf == null);
    }

    private static boolean loginSuccessful(String content) {
	try {
	    return (boolean) ((JSONObject) new JSONParser().parse(content)).get("success");
	} catch (ParseException | NullPointerException e) {
	    return false;
	}
    }

    private static String getRandomString(int length) {
	String random = "";
	String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
	for (int i = 0; i < length; i++)
	    random += chars.charAt(new Random().nextInt(chars.length()));
	return random;
    }
}