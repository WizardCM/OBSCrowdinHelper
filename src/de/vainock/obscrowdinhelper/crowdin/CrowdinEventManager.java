package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;

/**
 * A class which is used to register {@link CrowdinRequestFinishedEvent}s.
 * 
 * @author Vainock
 */
public class CrowdinEventManager {
	private static CrowdinEventManager manager;
	private List<CrowdinRequestFinishedEvent> events;

	private CrowdinEventManager() {

	}

	/**
	 * Registers a {@link CrowdinRequestFinishedEvent} which is going to be triggered when a {@link CrowdinRequest} sent with {@link CrowdinRequest#sendWithTrigger()}
	 * finishes.
	 * 
	 * @author Vainock
	 * @param event - A {@link CrowdinRequestFinishedEvent}.
	 */
	public void registerEvent(CrowdinRequestFinishedEvent event) {
		if (events == null)
			events = new ArrayList<CrowdinRequestFinishedEvent>();
		events.add(event);
	}

	/**
	 * Unregisters a {@link CrowdinRequestFinishedEvent} to be no longer triggered when a {@link CrowdinRequest} sent with {@link CrowdinRequest#sendWithTrigger()}
	 * finishes.
	 * 
	 * @author Vainock
	 * @param event - A {@link CrowdinRequestFinishedEvent}.
	 */
	public void unregisterEvent(CrowdinRequestFinishedEvent event) {
		events.remove(event);
	}

	void callEvents(CrowdinResponse response) {
		for (CrowdinRequestFinishedEvent event : events)
			event.requestFinishedEvent(response);
	}

	/**
	 * Returns the {@link CrowdinEventManager} intance which allows the registration of {@link CrowdinRequestFinishedEvent}s
	 * 
	 * @author Vainock
	 * @return the {@link CrowdinEventManager} intance.
	 */
	public static CrowdinEventManager getInstance() {
		if (manager == null)
			manager = new CrowdinEventManager();
		return manager;
	}
}