package vainock.crowdin;

public class CrowdinLogin {
    private CrowdinLogin() {

    }

    public static void login(String login, String password, String mfaCode) {
	CrowdinRequest req = new CrowdinRequest();
	req.setUrl("accounts.crowdin.com/login");
	req.setMethod(CrowdinRequestMethod.POST);
	req.addFormEntry("email_or_login", login);
	req.addFormEntry("password", password);
	if (!(mfaCode.isEmpty() || mfaCode == null))
	    req.addFormEntry("one_time_password", mfaCode);
	req.send();
	MyCookieJar.getInstance().clearCookiesFromLogin();
	CrowdinResponse.clearResponses();
    }

    public static boolean loginSuccessful() {
	CrowdinRequest req = new CrowdinRequest();
	req.setUrl("crowdin.com/profile");
	req.setMethod(CrowdinRequestMethod.GET);
	return req.send().getUrl().equals("https://" + req.getUrl());
    }
}