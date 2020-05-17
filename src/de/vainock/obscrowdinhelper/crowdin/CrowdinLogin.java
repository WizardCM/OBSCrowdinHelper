package de.vainock.obscrowdinhelper.crowdin;

/**
 * A class that takes over the login process to <a href="https://crowdin.com/">Crowdin</a> which is required for mostly all of the {@link CrowdinRequest}s.
 * 
 * @author Vainock
 */
public class CrowdinLogin {
	private CrowdinLogin() {

	}

	/**
	 * Logins to an account to contain specific cookies needed for mostly all of the {@link CrowdinRequest}s.
	 * 
	 * @author Vainock
	 * @param login    - A {@link String} representing the email or username of the account.
	 * @param password - A {@link String} representing the password of the account.
	 * @param tfaCode  - A {@link String} representing the Two-Factor Authentication code. If TFA is disabled, pass either an empty {@link String} or <code>null</code>.
	 */
	public static void login(String login, String password, String tfaCode) {
		if (login.isEmpty() || login == null || password.isEmpty() || password == null)
			return;
		CrowdinRequest req = new CrowdinRequest()
			.setUrl("accounts.crowdin.com/login")
			.setMethod(CrowdinRequestMethod.POST)
			.setFormEntry("email_or_login", login)
			.setFormEntry("password", password);
		if (!(tfaCode.isEmpty() || tfaCode == null))
			req.setFormEntry("one_time_password", tfaCode);
		req.send();
		CrowdinCookieJar.getInstance().clearCookiesFromLogin();
	}

	/**
	 * Returns if the login using {@link #login(String, String, String)} was successful.
	 * 
	 * @author Vainock
	 * @return a <code>boolean</code> if the login was successful.
	 */
	public static boolean loginSuccessful() {
		return !new CrowdinRequest().setUrl("crowdin.com/profile").setMethod(CrowdinRequestMethod.GET).send().isUrlDifferent();
	}
}