package com.nhl.link.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.query.PrefetchTreeNode;

import com.fasterxml.jackson.core.JsonGenerator;
import com.nhl.link.rest.encoder.Encoder;
import com.nhl.link.rest.encoder.GenericEncoder;

/**
 * {@link DataResponse} is populated with request parts as a request processing
 * pipeline proceeds. After all its information is filled, the response can be
 * encoded to JSON.
 */
public class DataResponse<T> extends SimpleResponse {

	private Class<T> type;
	private ClientEntity<T> entity;
	private int fetchOffset;
	private int fetchLimit;
	private int prefetchSemantics;
	private List<T> objects;
	private Encoder encoder;

	/**
	 * @since 6.9
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> DataResponse<T> forObject(T object) {

		if (object == null) {
			throw new NullPointerException("Null object");
		}

		Class<T> type = (Class<T>) object.getClass();
		return new DataResponse<>(type).withObjects(Collections.singletonList(object));
	}

	/**
	 * @since 6.9
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> DataResponse<T> forObjects(List<T> objects) {

		if (objects.isEmpty()) {
			return new DataResponse(Object.class);
		} else {
			Class<T> type = (Class<T>) objects.get(0).getClass();
			return new DataResponse<>(type).withObjects(objects);
		}
	}

	/**
	 * @since 6.9
	 */
	public static <T> DataResponse<T> forType(Class<T> type) {
		return new DataResponse<>(type);
	}

	DataResponse(Class<T> type) {
		super(true, null);
		this.type = type;
		this.prefetchSemantics = PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS;
		this.encoder = GenericEncoder.encoder();
		this.objects = Collections.emptyList();
	}

	/**
	 * @since 6.9
	 */
	public DataResponse<T> withMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * @since 6.9
	 */
	public Class<T> getType() {
		return type;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public ClientEntity<T> getEntity() {
		return entity;
	}

	public int getFetchOffset() {
		return fetchOffset;
	}

	public DataResponse<T> withFetchOffset(int fetchOffset) {
		this.fetchOffset = fetchOffset;
		return this;
	}

	public int getFetchLimit() {
		return fetchLimit;
	}

	public DataResponse<T> withFetchLimit(int fetchLimit) {
		this.fetchLimit = fetchLimit;
		return this;
	}

	public int getPrefetchSemantics() {
		return prefetchSemantics;
	}

	public DataResponse<T> withPrefetchSemantics(int prefetchSemantics) {
		this.prefetchSemantics = prefetchSemantics;
		return this;
	}

	/**
	 * @since 6.9
	 */
	public DataResponse<T> withClientEntity(ClientEntity<T> rootEntity) {
		this.entity = rootEntity;
		return this;
	}

	/**
	 * @since 6.9
	 */
	public DataResponse<T> withObjects(List<T> objects) {
		this.objects = objects;
		return this;
	}

	/**
	 * @since 6.9
	 */
	public DataResponse<T> withObject(T object) {
		this.objects = Collections.singletonList(object);
		return this;
	}

	/**
	 * Returns all objects returned from DB.
	 * 
	 * @since 6.9
	 */
	public List<T> getObjects() {
		return objects;
	}

	/**
	 * @since 6.9
	 */
	public DataResponse<T> withEncoder(Encoder encoder) {
		this.encoder = encoder;
		return this;
	}

	/**
	 * Writes internal state to the provided JSON stream using the internal
	 * {@link Encoder}.
	 * 
	 * @since 6.9
	 */
	public void writeData(JsonGenerator out) throws IOException {
		encoder.encode("data", getObjects(), out);
	}
}
