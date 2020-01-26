package de.vainock.obscrowdinhelper.crowdin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A class which simplifies the process of sending https requests to <a href="https://crowdin.com/">Crowdin</a>.
 * 
 * @since 1.0
 * @author Vainock
 */
public class CrowdinRequest implements Runnable {
	private static int maxRunningReqs = 50;
	private static final OkHttpClient httpClient = new OkHttpClient().newBuilder().followRedirects(true).cookieJar(CrowdinCookieJar.getInstance()).build();
	private Map<String, String> headers = new HashMap<>(), parameters = new HashMap<>(), formEntries = new HashMap<>();
	private String url = "crowdin.com";
	private CrowdinRequestMethod method;

	/**
	 * An object which simplifies the process of sending https requests to <a href="https://crowdin.com/">Crowdin</a>.
	 * 
	 * @since 1.0
	 * @author Vainock
	 */
	public CrowdinRequest() {

	}

	/**
	 * Sets a parameter of the {@link CrowdinRequest} which is later being converted into a <a href="https://en.wikipedia.org/wiki/Query_string">query string</a>.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name  - A {@link String} representing the name of the parameter.
	 * @param value - An {@link Object} representing the value of the parameter.
	 * @return the {@link CrowdinRequest} itself.
	 * @throws IllegalArgumentException if <i>name</i> is either empty or <code>null</code>.
	 */
	public CrowdinRequest setParam(String name, Object value) {
		if (name.isEmpty() || name == null)
			throw new IllegalArgumentException("The name of a form entry can neither be empty nor null.");
		this.parameters.put(name, String.valueOf(value));
		return this;
	}

	/**
	 * Removes a parameter from the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the parameter.
	 * @return the {@link CrowdinRequest} itself.
	 */
	public CrowdinRequest removeParam(String name) {
		this.parameters.remove(name);
		return this;
	}

	/**
	 * Returns the value of a parameter.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the parameter.
	 * @return a {@link String} representing the value of the parameter, or <code>null</code> if it doesn't exist.
	 */
	public String getParam(String name) {
		return parameters.get(name);
	}

	/**
	 * Returns a {@link Map} containing all parameters.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return a {@link Map}&lt;{@link String},{@link String}&gt; containing all parameters.
	 */
	public Map<String, String> getParams() {
		return this.parameters;
	}

	/**
	 * Sets the base url of the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param url - A {@link String} representing the base url of the {@link CrowdinRequest}. The default value is <code>crowdin.com</code>.
	 * @return the {@link CrowdinRequest} itself.
	 * @throws IllegalArgumentException if <i>url</i> is either empty or <code>null</code>.
	 */
	public CrowdinRequest setUrl(String url) {
		if (url.isEmpty() || url == null)
			throw new IllegalArgumentException("The url can't neither be empty nor null.");
		this.url = url;
		return this;
	}

	/**
	 * Returns the base url of the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return a {@link String} representing the base url of the {@link CrowdinRequest}.
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the {@link CrowdinRequestMethod} to be used for the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param method - The {@link CrowdinRequestMethod} to be used for the {@link CrowdinRequest}.
	 * @return the {@link CrowdinRequest} itself.
	 * @throws IllegalArgumentException if <i>method</i> is <code>null</code>.
	 */
	public CrowdinRequest setMethod(CrowdinRequestMethod method) {
		if (method == null)
			throw new IllegalArgumentException("The CrowdinRequestMethod can't be null.");
		this.method = method;
		return this;
	}

	/**
	 * Returns the {@link CrowdinRequestMethod} to be used for the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return the {@link CrowdinRequestMethod} to be used for the {@link CrowdinRequest}, or <code>null</code> if it doesn't exist.
	 */
	public CrowdinRequestMethod getMethod() {
		return this.method;
	}

	void setHeader(String name, String value) {
		this.headers.put(name, value);
	}

	String getHeader(String name) {
		return headers.get(name);
	}

	/**
	 * Sets a form entry for requests with {@link CrowdinRequestMethod#POST} which is later being converted to full form data.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name  - A {@link String} representing the name of the form entry.
	 * @param value - An {@link Object} representing the value of the form entry.
	 * @return the {@link CrowdinRequest} itself.
	 * @throws IllegalArgumentException if <i>name</i> is either empty or <code>null</code>.
	 */
	public CrowdinRequest setFormEntry(String name, Object value) {
		if (name.isEmpty() || name == null)
			throw new IllegalArgumentException("The name of a form entry can neither be empty nor null.");
		formEntries.put(name, String.valueOf(value));
		return this;
	}

	/**
	 * Removes a form entry from the {@link CrowdinRequest}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the form entry.
	 * @return the {@link CrowdinRequest} itself.
	 */
	public CrowdinRequest removeFormEntry(String name) {
		formEntries.remove(name);
		return this;
	}

	/**
	 * Returns the value of a form entry.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the form entry.
	 * @return a {@link String} representing the value of a form entry, or <code>null</code> if it doesn't exist.
	 */
	public String getFormEntry(String name) {
		return formEntries.get(name);
	}

	/**
	 * Returns a {@link Map} containing all form entries.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return a {@link Map}&lt;{@link String},{@link String}&gt; containing all parameters.
	 */
	public Map<String, String> getFormEntries() {
		return formEntries;
	}

	/**
	 * Returns the amount of parallel running {@link CrowdinRequest}s used by {@link #send(List)}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return an <code>int</code> representing the amount of parallel running {@link CrowdinRequest}s.
	 */
	public static int getMaxRunningRequests() {
		return maxRunningReqs;
	}

	/**
	 * Sets the amount of parallel running {@link CrowdinRequest}s used by {@link #send(List)}.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param maxRunningRequests - An <code>int</code> representing the amount of parallel running {@link CrowdinRequest}s. The default value is <code>50</code>.
	 * @throws IllegalArgumentException if <i>maxRunningRequests</i> is lower than 1.
	 */
	public static void setMaxRunningRequests(int maxRunningRequests) {
		if (maxRunningRequests < 1)
			throw new IllegalArgumentException("The amount of parallel running requests must be at least 1. Specified: " + maxRunningRequests);
		maxRunningReqs = maxRunningRequests;
	}

	/**
	 * Sends the current {@link CrowdinRequest} and waits for it to finish.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @return the {@link CrowdinResponse}.
	 */
	public CrowdinResponse send() {
		new Thread(this).run();
		return CrowdinResponse.getResponses().get(0);
	}

	/**
	 * Sends all specified {@link CrowdinRequest}s using the amount of parallel {@link CrowdinRequest}s defined with {@link #setMaxRunningRequests(int)} and waits for them
	 * to finish.
	 * 
	 * @since 1.0
	 * @author Vainock
	 * @param requests - A {@link List}&lt;{@link CrowdinRequest}&gt;.
	 * @return an unsorted {@link List}&lt;{@link CrowdinResponse}&gt;.
	 * @throws IllegalArgumentException if <i>requests</i> is <code>null</code>.
	 */
	public static List<CrowdinResponse> send(List<CrowdinRequest> requests) {
		if (requests == null)
			throw new IllegalArgumentException("The list of requests can't be null.");
		if (requests.size() == 0)
			return new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(maxRunningReqs);
		for (CrowdinRequest request : requests)
			executor.execute(request);
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		return CrowdinResponse.getResponses();
	}

	@Override
	public void run() {
		if (method == null)
			throw new IllegalArgumentException("The CrowdinRequestMethod can't be null.");
		StringBuilder resultSb = new StringBuilder();
		resultSb.append("https://");
		resultSb.append(url);
		resultSb.append('?');
		for (Entry<String, String> entry : parameters.entrySet())
			try {
				resultSb.append(URLEncoder.encode(entry.getKey(), "utf-8"));
				resultSb.append('=');
				resultSb.append(URLEncoder.encode(entry.getValue(), "utf-8"));
				resultSb.append('&');
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		resultSb.setLength(resultSb.length() - 1);
		Request.Builder reqBuilder = new Request.Builder();
		reqBuilder.url(resultSb.toString());
		for (Entry<String, String> entry : headers.entrySet())
			reqBuilder.header(entry.getKey(), entry.getValue());
		for (Cookie cookie : CrowdinCookieJar.getInstance().getCookies())
			if (cookie.name().equals("csrf_token")) {
				reqBuilder.header("x-csrf-token", cookie.value());
				break;
			}
		if (method.equals(CrowdinRequestMethod.POST)) {
			FormBody.Builder formBody = new FormBody.Builder();
			for (Entry<String, String> entry : formEntries.entrySet())
				formBody.add(entry.getKey(), entry.getValue());
			reqBuilder.post(formBody.build());
		} else
			reqBuilder.get();
		try {
			Response response = httpClient.newCall(reqBuilder.build()).execute();
			CrowdinResponse res = new CrowdinResponse();
			res.setContent(response.body().string());
			if (response.request().url().toString().equals("https://" + url))
				res.setUrlDifferent(false);
			CrowdinResponse.addResponse(res);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}