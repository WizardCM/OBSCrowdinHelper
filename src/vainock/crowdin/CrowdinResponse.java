package vainock.crowdin;

import java.util.HashMap;

public class CrowdinResponse {

    private String content;
    private HashMap<String, String> cookies = new HashMap<>();

    CrowdinResponse() {

    }

    CrowdinResponse setContent(String content) {
	this.content = content;
	return this;
    }

    public String getContent() {
	return this.content;
    }

    CrowdinResponse addCookie(String name, String value) {
	this.cookies.put(name, value);
	return this;
    }

    String getCookie(String name) {
	return this.cookies.get(name);
    }
}