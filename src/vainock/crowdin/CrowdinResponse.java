package vainock.crowdin;

import java.util.ArrayList;

public class CrowdinResponse {
    private static ArrayList<CrowdinResponse> responses = new ArrayList<>();
    private String content;
    private String url;

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

    public static ArrayList<CrowdinResponse> getResponses() {
	ArrayList<CrowdinResponse> res = responses;
	responses.clear();
	return res;
    }

    public static void clearResponses() {
	responses.clear();
    }

    synchronized static void addResponse(CrowdinResponse response) {
	responses.add(response);
    }

    static CrowdinResponse getFirstResponse() {
	return responses.get(0);
    }
}