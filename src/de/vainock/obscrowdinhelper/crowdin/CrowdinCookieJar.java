package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CrowdinCookieJar implements CookieJar {
	private static CrowdinCookieJar myCookieJar = new CrowdinCookieJar();
	private List<Cookie> cookies = new ArrayList<>();

	private CrowdinCookieJar() {
		StringBuilder sb = new StringBuilder();
		String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
		for (int i = 0; i < 10; i++)
			sb.append(chars.charAt(new Random().nextInt(chars.length())));
		cookies.add(new Cookie.Builder().name("csrf_token").value(sb.toString()).domain("crowdin.com").path("/").build());
	}

	public static CrowdinCookieJar getInstance() {
		return myCookieJar;
	}

	@Override
	public List<Cookie> loadForRequest(HttpUrl url) {
		if (cookies == null)
			return new ArrayList<>();
		return cookies;
	}

	@Override
	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		for (Cookie cookie : cookies)
			this.cookies.add(cookie);
	}

	void clearCookiesFromLogin() {
		List<Cookie> cookiesLeft = new ArrayList<>();
		for (Cookie cookie : cookies)
			if (cookie.name().equals("cid") || cookie.name().equals("token") || cookie.name().equals("csrf_token"))
				cookiesLeft.add(cookie);
		cookies = cookiesLeft;
	}

	public void setCookies(List<Cookie> cookies) {
		this.cookies = cookies;
	}

	public List<Cookie> getCookies() {
		return cookies;
	}
}