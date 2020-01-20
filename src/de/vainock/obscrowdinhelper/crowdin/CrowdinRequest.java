package de.vainock.obscrowdinhelper.crowdin;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CrowdinRequest implements Runnable {
	private static int maxRunningReqs = 50;
	private final OkHttpClient httpClient = new OkHttpClient().newBuilder().followRedirects(true)
			.cookieJar(CrowdinCookieJar.getInstance()).build();
	private Map<String, String> headers = new HashMap<>(), parameters = new HashMap<>(), formEntries = new HashMap<>();
	private String url;
	private CrowdinRequestMethod method;

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

	public Map<String, String> getParams() {
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

	public CrowdinRequest addFormEntry(String name, Object value) {
		formEntries.put(name, value.toString());
		return this;
	}

	public CrowdinRequest removeFormEntry(String name) {
		formEntries.remove(name);
		return this;
	}

	public String getFormEntry(String name) {
		return formEntries.get(name);
	}

	public Map<String, String> getFormEntries() {
		return formEntries;
	}

	public static List<CrowdinResponse> send(List<CrowdinRequest> requests) {
		ExecutorService executor = Executors.newFixedThreadPool(maxRunningReqs);
		for (CrowdinRequest request : requests)
			executor.execute(request);
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		return CrowdinResponse.getResponses();
	}

	public CrowdinResponse send() {
		new Thread(this).run();
		return CrowdinResponse.getResponses().get(0);
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
			reqBuilder.header("x-csrf-token", CrowdinCookieJar.getInstance().getCookieValue("csrf_token"));
			if (method.equals(CrowdinRequestMethod.POST)) {
				FormBody.Builder formBody = new FormBody.Builder();
				for (Entry<String, String> entry : formEntries.entrySet())
					formBody.add(entry.getKey(), entry.getValue());
				reqBuilder.post(formBody.build());
			} else
				reqBuilder.get();
			Response response = httpClient.newCall(reqBuilder.build()).execute();
			CrowdinResponse.addResponse(
					new CrowdinResponse().setContent(response.body().string()).setUrl(response.request().url().toString()));
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
}