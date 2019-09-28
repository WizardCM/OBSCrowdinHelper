package vainock.crowdin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

public class CrowdinRequest implements Runnable {

    private static ArrayList<CrowdinResponse> responses = new ArrayList<>();
    private static int maxRunningReqs = 1;
    private static int runningReqs = 0;
    private HashMap<String, String> properties = new HashMap<>();
    private HashMap<String, String> parameters = new HashMap<>();
    private String url;
    private HttpRequestMethod method;
    private Thread reqThread;

    public CrowdinRequest() {

    }

    public CrowdinRequest addParam(String name, String value) {
	this.parameters.put(name, value);
	return this;
    }

    public CrowdinRequest removeParam(String name) {
	this.parameters.remove(name);
	return this;
    }

    public HashMap<String, String> getParams() {
	return this.parameters;
    }

    public CrowdinRequest setUrl(String url) {
	this.url = url;
	return this;
    }

    public String getUrl() {
	return this.url;
    }

    public CrowdinRequest setMethod(HttpRequestMethod method) {
	this.method = method;
	return this;
    }

    public HttpRequestMethod getMethod() {
	return this.method;
    }

    CrowdinRequest setProperty(String name, String value) {
	this.properties.put(name, value);
	return this;
    }

    String getProperty(String name) {
	return properties.get(name);
    }

    public CrowdinRequest send() {
	while (runningReqs >= maxRunningReqs)
	    waitMoment();
	requestStarted();
	reqThread = new Thread(this);
	reqThread.start();
	return this;
    }

    @Override
    public void run() {
	CrowdinResponse response = new CrowdinResponse();
	try {
	    StringBuilder resultSb = new StringBuilder();
	    resultSb.append('?');
	    for (Entry<String, String> entry : parameters.entrySet()) {
		try {
		    resultSb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
		    resultSb.append("=");
		    resultSb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		    resultSb.append("&");
		} catch (UnsupportedEncodingException e) {
		    e.printStackTrace();
		}
	    }
	    resultSb.setLength(resultSb.length() - 1);

	    URL url = new URL("https://crowdin.com/" + this.url + resultSb);
	    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
	    con.setRequestMethod(this.method.toString());
	    if (properties.size() > 0) {
		for (String key : properties.keySet())
		    con.setRequestProperty(key, properties.get(key));
	    } else {
		con.setRequestProperty("cookie",
			"csrf_token=" + CrowdinLogin.login_csrf + "; cid=" + CrowdinLogin.login_cid);
		con.setRequestProperty("x-csrf-token", CrowdinLogin.login_csrf);
	    }

	    con.setDoOutput(true);

	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    String inputLine;
	    StringBuilder contentSb = new StringBuilder();
	    while ((inputLine = in.readLine()) != null)
		contentSb.append(inputLine + "\n");

	    in.close();
	    response.setContent(contentSb.toString());

	    if (con.getHeaderField("Set-Cookie") != null) {
		String cookiesHeader = con.getHeaderField("Set-Cookie");
		List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
		for (HttpCookie cookie : cookies)
		    response.addCookie(cookie.getName(), cookie.getValue());
	    }

	    con.disconnect();

	} catch (IOException e) {
	    e.printStackTrace();
	}

	addCrowdinResponse(response);
	requestFinished();
    }

    public static int getMaxRunningRequests() {
	return maxRunningReqs;
    }

    public static void setMaxRunningRequests(int maxRunningRequests) {
	maxRunningReqs = maxRunningRequests;
    }

    public static int getRunningRequests() {
	return runningReqs;
    }

    private synchronized void requestFinished() {
	runningReqs--;
    }

    private synchronized void requestStarted() {
	runningReqs++;
    }

    public CrowdinResponse getCrowdinResponse() {
	clearCrowdinResponses();
	try {
	    reqThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
	return responses.get(0);
    }

    public static ArrayList<CrowdinResponse> getCrowdinResponses() {
	return responses;
    }

    public static void clearCrowdinResponses() {
	responses.clear();
    }

    private synchronized void addCrowdinResponse(CrowdinResponse crowdinResponse) {
	responses.add(crowdinResponse);
    }

    public static void waitForEveryRequest() {
	while (runningReqs != 0)
	    waitMoment();
    }

    private static void waitMoment() {
	try {
	    Thread.currentThread();
	    Thread.sleep(50);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
}