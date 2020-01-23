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
		cookies.add(new Cookie.Builder().name("csrf_token").value(getRandomString(10)).domain("crowdin.com").path("/").build());
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

	String getCookieValue(String name) {
		for (Cookie cookie : cookies)
			if (cookie.name().equals(name))
				return cookie.value();
		return null;
	}

	void clearCookiesFromLogin() {
		List<Cookie> cookiesLeft = new ArrayList<>();
		for (Cookie cookie : cookies)
			if (cookie.name().equals("cid") || cookie.name().equals("token") || cookie.name().equals("csrf_token"))
				cookiesLeft.add(cookie);
		cookies = cookiesLeft;
	}

	private String getRandomString(int length) {
		String random = "";
		String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
		for (int i = 0; i < length; i++)
			random += chars.charAt(new Random().nextInt(chars.length()));
		return random;
	}

	public String getCookiesRaw() {
		StringBuilder sb = new StringBuilder();
		for (Cookie cookie : cookies)
			sb.append(cookie.name() + ";" + cookie.value() + ";" + cookie.domain() + "\n");
		return sb.toString();
	}

	public void setCookies(String rawCookies) {
		List<Cookie> cookies = new ArrayList<>();
		for (String cookieRaw : rawCookies.split("\n")) {
			String[] cookie = cookieRaw.split(";");
			cookies.add(new Cookie.Builder().name(cookie[0]).value(cookie[1]).domain(cookie[2]).path("/").build());
		}
		this.cookies = cookies;
	}
}