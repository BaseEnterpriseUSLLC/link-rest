package com.nhl.link.rest.runtime;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import org.apache.cayenne.di.Injector;

public class LinkRestRuntime {

	static final String LINK_REST_CONTAINER_PROPERTY = "linkrest.container";

	private Feature feature;
	private Injector injector;

	/**
	 * Returns a LinkRest service of a specified type.
	 */
	public static <T> T service(Class<T> type, Configuration config) {

		Injector injector = (Injector) config.getProperty(LINK_REST_CONTAINER_PROPERTY);
		if (injector == null) {
			throw new IllegalStateException("LinkRest is misconfigured. No injector found for property: "
					+ LINK_REST_CONTAINER_PROPERTY);
		}

		return injector.getInstance(type);
	}

	LinkRestRuntime(Feature feature, Injector injector) {
		this.feature = feature;
		this.injector = injector;
	}

	public Feature getFeature() {
		return feature;
	}

	public <T> T service(Class<T> type) {
		return injector.getInstance(type);
	}

}
