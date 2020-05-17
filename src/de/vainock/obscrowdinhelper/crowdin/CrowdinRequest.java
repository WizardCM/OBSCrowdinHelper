package de.vainock.obscrowdinhelper.crowdin;

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
 * @author Vainock
 */
public class CrowdinRequest implements Runnable {
	private static int maxRunningReqs = 25;
	private static ExecutorService executor;
	private static OkHttpClient httpClient;
	private Map<String, String> headers = new HashMap<String, String>(), parameters = new HashMap<String, String>(), formEntries = new HashMap<String, String>();
	private String url;
	private CrowdinRequestMethod method;
	private boolean trigger;

	/**
	 * An object which simplifies the process of sending https requests to <a href="https://crowdin.com/">Crowdin</a>.
	 * 
	 * @author Vainock
	 */
	public CrowdinRequest() {

	}

	String getHeader(String name) {
		return headers.get(name);
	}

	CrowdinRequest setHeader(String name, String value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Returns the value of a parameter.
	 * 
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
	 * @author Vainock
	 * @return a {@link Map}&lt;{@link String},{@link String}&gt; containing all parameters.
	 */
	public Map<String, String> getParams() {
		return this.parameters;
	}

	/**
	 * Sets a parameter of the {@link CrowdinRequest} which is later being converted into a <a href="https://en.wikipedia.org/wiki/Query_string">query string</a>.
	 * 
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
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the parameter.
	 * @return the {@link CrowdinRequest} itself.
	 */
	public CrowdinRequest removeParam(String name) {
		this.parameters.remove(name);
		return this;
	}

	/**
	 * Returns the url of the {@link CrowdinRequest}.
	 * 
	 * @author Vainock
	 * @return a {@link String} representing the url of the {@link CrowdinRequest} without <code>https://www.</code> at the beginning.<br>
	 *         <b>Example: </b><code>accounts.crowdin.com/login</code>
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Sets the url of the {@link CrowdinRequest}.
	 * 
	 * @author Vainock
	 * @param url - A {@link String} representing the url of the {@link CrowdinRequest} without <code>https://www.</code> at the beginning.<br>
	 *            <b>Example: </b><code>accounts.crowdin.com/login</code>
	 * @return the {@link CrowdinRequest} itself.
	 * @throws IllegalArgumentException if <i>url</i> is either empty or <code>null</code>.
	 */
	public CrowdinRequest setUrl(String url) {
		if (url.isEmpty() || url == null)
			throw new IllegalArgumentException("The url can neither be empty nor null.");
		this.url = url;
		return this;
	}

	/**
	 * Returns the {@link CrowdinRequestMethod} which is going to be used for the {@link CrowdinRequest}.
	 * 
	 * @author Vainock
	 * @return the {@link CrowdinRequestMethod} which is going to be used for the {@link CrowdinRequest}, or <code>null</code> if it doesn't exist.
	 */
	public CrowdinRequestMethod getMethod() {
		return this.method;
	}

	/**
	 * Sets the {@link CrowdinRequestMethod} which is going to be used for the {@link CrowdinRequest}.
	 * 
	 * @author Vainock
	 * @param method - The {@link CrowdinRequestMethod} which is going to be used for the {@link CrowdinRequest}.
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
	 * Returns the value of a form entry.
	 * 
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
	 * @author Vainock
	 * @return a {@link Map}&lt;{@link String},{@link String}&gt; containing all parameters.
	 */
	public Map<String, String> getFormEntries() {
		return formEntries;
	}

	/**
	 * Sets a form entry for requests with {@link CrowdinRequestMethod#POST} which is later being converted to full form data.
	 * 
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
	 * @author Vainock
	 * @param name - A {@link String} representing the name of the form entry.
	 * @return the {@link CrowdinRequest} itself.
	 */
	public CrowdinRequest removeFormEntry(String name) {
		formEntries.remove(name);
		return this;
	}

	/**
	 * Returns the amount of parallel running {@link CrowdinRequest}s used by {@link #send(List)} and {@link #sendWithTrigger()}.
	 * 
	 * @author Vainock
	 * @return an <code>int</code> representing the amount of parallel running {@link CrowdinRequest}s.
	 */
	public static int getMaxRunningRequests() {
		return maxRunningReqs;
	}

	/**
	 * Sets the amount of parallel running {@link CrowdinRequest}s used by {@link #send(List)} and {@link #sendWithTrigger()}.<br>
	 * <br>
	 * <b>Shouldn't be used when any {@link CrowdinRequest} is still running.</b><br>
	 * Use {@link #shutdown(boolean)} to ensure no request is running.
	 * 
	 * @author Vainock
	 * @param maxRunningRequests - An <code>int</code> representing the amount of parallel running {@link CrowdinRequest}s. The default value is <code>25</code>.
	 * @throws IllegalArgumentException if <i>maxRunningRequests</i> is lower than 1.
	 */
	public static void setMaxRunningRequests(int maxRunningRequests) {
		if (maxRunningRequests < 1)
			throw new IllegalArgumentException("The amount of parallel running requests must be at least 1. Specified: " + maxRunningRequests);
		executor = Executors.newFixedThreadPool(maxRunningRequests);
	}

	/**
	 * Sends the current {@link CrowdinRequest} and waits for it to finish.
	 * 
	 * @author Vainock
	 * @return the {@link CrowdinResponse}.
	 */
	public synchronized CrowdinResponse send() {
		new Thread(this).run();
		return CrowdinResponse.getResponses().get(0);
	}

	/**
	 * Sends the current {@link CrowdinRequest} using the amount of parallel {@link CrowdinRequest}s defined with {@link #setMaxRunningRequests(int)} and when finished
	 * triggers {@link CrowdinRequestFinishedEvent#requestFinishedEvent(CrowdinResponse)} if registered with
	 * {@link CrowdinEventManager#registerEvent(CrowdinRequestFinishedEvent)}.
	 * 
	 * @author Vainock
	 */
	public synchronized void sendWithTrigger() {
		trigger = true;
		if (executor == null)
			executor = Executors.newFixedThreadPool(maxRunningReqs);
		executor.execute(new Thread(this));
	}

	/**
	 * Sends all specified {@link CrowdinRequest}s using the amount of parallel {@link CrowdinRequest}s defined with {@link #setMaxRunningRequests(int)} and waits for them
	 * to finish.
	 * 
	 * @author Vainock
	 * @param requests - A {@link List}&lt;{@link CrowdinRequest}&gt;.
	 * @return an unsorted {@link List}&lt;{@link CrowdinResponse}&gt;.
	 * @throws IllegalArgumentException if <i>requests</i> is <code>null</code>.
	 */
	public static List<CrowdinResponse> send(List<CrowdinRequest> requests) {
		if (requests == null)
			throw new IllegalArgumentException("The list of requests can't be null.");
		if (requests.size() == 0)
			return new ArrayList<CrowdinResponse>();
		if (executor == null)
			executor = Executors.newFixedThreadPool(maxRunningReqs);
		for (CrowdinRequest request : requests)
			executor.execute(request);
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		return CrowdinResponse.getResponses();
	}

	@Override
	public void run() {
		try {
			if (method == null)
				throw new IllegalArgumentException("The CrowdinRequestMethod can't be null.");
			if (url == null)
				throw new IllegalArgumentException("The url can't be null.");
			StringBuilder resultSb = new StringBuilder();
			resultSb.append("https://");
			resultSb.append(url);
			resultSb.append('?');
			for (Entry<String, String> entry : parameters.entrySet()) {
				resultSb.append(URLEncoder.encode(entry.getKey(), "utf-8"));
				resultSb.append('=');
				resultSb.append(URLEncoder.encode(entry.getValue(), "utf-8"));
				resultSb.append('&');
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
			if (httpClient == null)
				httpClient = new OkHttpClient().newBuilder().followRedirects(true).cookieJar(CrowdinCookieJar.getInstance()).build();
			Response response = httpClient.newCall(reqBuilder.build()).execute();
			CrowdinResponse res = new CrowdinResponse().setContent(response.body().string()).setCrowdinRequest(this).setStatusCode(response.code());
			if (response.request().url().toString().equals("https://" + url))
				res.setUrlDifferent(false);
			if (trigger)
				CrowdinEventManager.getInstance().callEvents(res);
			else
				CrowdinResponse.addResponse(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets all internal variables to <code>null</code> to reduce memory usage. If the variables are being needed again, they are getting re-created.
	 * 
	 * @author Vainock
	 * @param waitForRequests - A <code>boolean</code> representing if the method should either wait for all running requests to finish or if it should cancel all
	 *                        requests.
	 */
	public static void shutdown(boolean waitForRequests) {
		if (executor != null) {
			if (waitForRequests)
				executor.shutdown();
			else
				executor.shutdownNow();
			executor = null;
		}
		httpClient = null;
		CrowdinResponse.responses = null;
	}
}