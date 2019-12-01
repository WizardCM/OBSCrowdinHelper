package vainock.crowdin;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map.Entry;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CrowdinRequest implements Runnable {
    private static int maxRunningReqs = 50;
    private static int runningReqs = 0;
    private final OkHttpClient httpClient = new OkHttpClient().newBuilder().followRedirects(true)
	    .cookieJar(MyCookieJar.getInstance()).build();
    private HashMap<String, String> headers = new HashMap<>();
    private HashMap<String, String> parameters = new HashMap<>();
    private HashMap<String, String> formEntries = new HashMap<>();
    private String url;
    private CrowdinRequestMethod method;
    private Thread reqThread;

    public CrowdinRequest addParam(String name, String value) {
	this.parameters.put(name, value);
	return this;
    }

    public CrowdinRequest removeParam(String name) {
	this.parameters.remove(name);
	return this;
    }

    public String getParam(String name) {
	return parameters.get(name);
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

    public CrowdinRequest setMethod(CrowdinRequestMethod method) {
	this.method = method;
	return this;
    }

    public CrowdinRequestMethod getMethod() {
	return this.method;
    }

    void setHeader(String name, String value) {
	this.headers.put(name, value);
    }

    String getHeader(String name) {
	return headers.get(name);
    }

    public CrowdinRequest addFormEntry(String name, String value) {
	formEntries.put(name, value);
	return this;
    }

    public CrowdinRequest removeFormEntry(String name) {
	formEntries.remove(name);
	return this;
    }

    public String getFormEntry(String name) {
	return formEntries.get(name);
    }

    public HashMap<String, String> getFormEntries() {
	return formEntries;
    }

    public void sendMultiple() {
	while (runningReqs >= maxRunningReqs)
	    waitMoment();
	requestStarted();
	reqThread = new Thread(this);
	reqThread.start();
    }

    public CrowdinResponse send() {
	while (runningReqs >= maxRunningReqs)
	    waitMoment();
	requestStarted();
	reqThread = new Thread(this);
	reqThread.start();
	CrowdinResponse.clearResponses();
	try {
	    reqThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
	return CrowdinResponse.getFirstResponse();
    }

    @Override
    public void run() {
	try {
	    StringBuilder resultSb = new StringBuilder();
	    resultSb.append('?');
	    for (Entry<String, String> entry : parameters.entrySet()) {
		resultSb.append(URLEncoder.encode(entry.getKey(), "utf-8"));
		resultSb.append("=");
		resultSb.append(URLEncoder.encode(entry.getValue(), "utf-8"));
		resultSb.append("&");
	    }
	    resultSb.setLength(resultSb.length() - 1);
	    Request.Builder reqBuilder = new Request.Builder();
	    reqBuilder.url("https://" + url + resultSb);
	    for (Entry<String, String> entry : headers.entrySet())
		reqBuilder.header(entry.getKey(), entry.getValue());
	    reqBuilder.header("x-csrf-token", MyCookieJar.getInstance().getCookieValue("csrf_token"));
	    if (method.equals(CrowdinRequestMethod.POST)) {
		FormBody.Builder formBody = new FormBody.Builder();
		for (Entry<String, String> entry : formEntries.entrySet())
		    formBody.add(entry.getKey(), entry.getValue());
		reqBuilder.post(formBody.build());
	    } else
		reqBuilder.get();
	    Response response = httpClient.newCall(reqBuilder.build()).execute();
	    CrowdinResponse.addResponse(new CrowdinResponse().setContent(response.body().string())
		    .setUrl(response.request().url().toString()));
	    requestFinished();
	} catch (Exception e) {
	    e.printStackTrace();
	}
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