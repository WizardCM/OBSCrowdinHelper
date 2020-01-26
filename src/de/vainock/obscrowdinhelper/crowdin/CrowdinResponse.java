package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A class which represents the response of a {@link CrowdinRequest}.
 * 
 * @since 1.0
 * @author Vainock
 */
public class CrowdinResponse {
	private static List<CrowdinResponse> responses = new ArrayList<>();
	private String content;
	private boolean differentUrl = true;

	/**
	 * An object which represents the response of a {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 */
	CrowdinResponse() {

	}

	void setContent(String content) {
		this.content = content;
	}

	/**
	 * Returns the response text to the {@link CrowdinRequest} which (should) represent information about the requested data. If the {@link CrowdinRequest} was successful,
	 * the content is likely a {@link JSONObject} or a {@link JSONArray}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return the response text to the {@link CrowdinRequest}.
	 */
	public String getContent() {
		return content;
	}

	void setUrlDifferent(boolean differentUrl) {
		this.differentUrl = differentUrl;
	}

	boolean isUrlDifferent() {
		return differentUrl;
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