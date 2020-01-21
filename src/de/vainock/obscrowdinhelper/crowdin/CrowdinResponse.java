package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;

public class CrowdinResponse {
	private static List<CrowdinResponse> responses = new ArrayList<>();
	private String content, url;

	CrowdinResponse() {

	}

	CrowdinResponse setContent(String content) {
		this.content = content;
		return this;
	}

	public String getContent() {
		return content;
	}

	CrowdinResponse setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getUrl() {
		return url;
	}

	static List<CrowdinResponse> getResponses() {
		List<CrowdinResponse> res = new ArrayList<>(responses);
		responses.clear();
		return res;
	}

	synchronized static void addResponse(CrowdinResponse response) {
		responses.add(response);
	}
}