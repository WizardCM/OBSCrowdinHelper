package de.vainock.obscrowdinhelper.crowdin;

/**
 * A class that takes over the login process to <a href="https://crowdin.com/">Crowdin</a> which is required for mostly all of the {@link CrowdinRequest}s.
 * 
 * @since 1.0
 * @author Vainock
 */
public class CrowdinLogin {
	private CrowdinLogin() {

	}

	/**
	 * Logins to an account using <b>accounts.crowdin.com/login</b> to contain specific cookies needed for mostly all of the {@link CrowdinRequest}s.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param login    - A {@link String} representing the email or username of the account.
	 * @param password - A {@link String} representing the password of the account.
	 * @param tfaCode  - A {@link String} representing the Two-Factor Authentication code. If TFA is disabled, pass either an empty {@link String} or <code>null</code>.
	 * @throws IllegalArgumentException if the login or password is either empty or <code>null</code>.
	 */
	public static void login(String login, String password, String tfaCode) {
		if (login.isEmpty() || login == null || password.isEmpty() || password == null)
			throw new IllegalArgumentException("The login and password can neither be empty nor null.");
		CrowdinRequest req = new CrowdinRequest();
		req.setUrl("accounts.crowdin.com/login");
		req.setMethod(CrowdinRequestMethod.POST);
		req.setFormEntry("email_or_login", login);
		req.setFormEntry("password", password);
		if (!(tfaCode.isEmpty() || tfaCode == null))
			req.setFormEntry("one_time_password", tfaCode);
		req.send();
		CrowdinCookieJar.getInstance().clearCookiesFromLogin();
	}

	/**
	 * Returns if the login using {@link #login(String, String, String)} was successful.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return a <code>boolean</code> if the login was successful.
	 */
	public static boolean loginSuccessful() {
		CrowdinRequest req = new CrowdinRequest();
		req.setUrl("crowdin.com/profile");
		req.setMethod(CrowdinRequestMethod.GET);
		return !req.send().isUrlDifferent();
	}
}