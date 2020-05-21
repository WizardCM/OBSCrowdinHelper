package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A class which represents the response of a {@link CrowdinRequest}.
 * 
 * @author Vainock
 */
public class CrowdinResponse {
  static List<CrowdinResponse> responses;
  private String content;
  private boolean differentUrl = true;
  private CrowdinRequest request;
  private int statusCode;

  /**
   * An object which represents the response of a {@link CrowdinRequest}.
   * 
   * @author Vainock
   */
  CrowdinResponse() {

  }

  CrowdinResponse setUrlDifferent(boolean differentUrl) {
    this.differentUrl = differentUrl;
    return this;
  }

  boolean isUrlDifferent() {
    return differentUrl;
  }

  /**
   * Returns the status code of the {@link CrowdinRequest}.
   * 
   * @author Vainock
   * @return an <code>int</code> representing the status code of the {@link CrowdinRequest}.
   */

  public int getStatusCode() {
    return statusCode;
  }

  CrowdinResponse setStatusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * Returns the response text to the {@link CrowdinRequest} which (should) represent information
   * about the requested data. If the {@link CrowdinRequest} was successful, the content is likely a
   * {@link JSONObject} or a {@link JSONArray}.
   * 
   * @author Vainock
   * @return a {@link String} representing the response text to the {@link CrowdinRequest}.
   */
  public String getContent() {
    return content;
  }

  CrowdinResponse setContent(String content) {
    this.content = content;
    return this;
  }

  static List<CrowdinResponse> getResponses() {
    List<CrowdinResponse> res = new ArrayList<CrowdinResponse>(responses);
    responses.clear();
    return res;
  }

  synchronized static void addResponse(CrowdinResponse response) {
    if (responses == null)
      responses = new ArrayList<CrowdinResponse>();
    responses.add(response);
  }

  /**
   * Returns the {@link CrowdinRequest} of the {@link CrowdinResponse}.
   * 
   * @author Vainock
   * @return the {@link CrowdinRequest}.
   */
  public CrowdinRequest getCrowdinRequest() {
    return request;
  }

  CrowdinResponse setCrowdinRequest(CrowdinRequest request) {
    this.request = request;
    return this;
  }
}
