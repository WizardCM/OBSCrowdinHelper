package de.vainock.obscrowdinhelper.crowdin;

/**
 * An interface which provides {@link #requestFinishedEvent(CrowdinResponse)}.
 * 
 * @author Vainock
 */
public interface CrowdinRequestFinishedEvent {
	/**
	 * Is being triggered when a {@link CrowdinRequest} sent with {@link CrowdinRequest#sendWithTrigger()} finishes.
	 * 
	 * @author Vainock
	 * @param response - The {@link CrowdinResponse} of the {@link CrowdinRequest}.
	 */
	void requestFinishedEvent(CrowdinResponse response);
}