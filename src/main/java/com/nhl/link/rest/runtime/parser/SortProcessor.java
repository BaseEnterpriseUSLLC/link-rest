package com.nhl.link.rest.runtime.parser;

import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhl.link.rest.ClientEntity;
import com.nhl.link.rest.LinkRestException;

class SortProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SortProcessor.class);

	private static final String ASC = "ASC";
	private static final String DESC = "DESC";

	private static final String PROPERTY = "property";
	private static final String DIRECTION = "direction";

	private RequestJsonParser jsonParser;
	private PathCache pathCache;

	SortProcessor(RequestJsonParser jsonParser, PathCache pathCache) {
		this.jsonParser = jsonParser;
		this.pathCache = pathCache;
	}

	void process(ClientEntity<?> clientEntity, String sort, String direction) {

		if (sort == null || sort.length() == 0) {
			return;
		}

		if (sort.startsWith("[")) {
			processSorter(clientEntity, sort);
		} else {
			processSimpleSorter(clientEntity, sort, direction);
		}
	}

	void processSimpleSorter(ClientEntity<?> clientEntity, String sort, String direction) {

		// TODO: do we need to support nested ID?
		ObjEntity entity = clientEntity.getEntity();

		// note using "toString" instead of "getPath" to convert ASTPath to
		// String representation. This ensures "db:" prefix is preserved if
		// present
		sort = pathCache.entityPathCache(entity).getPathDescriptor(new ASTObjPath(sort)).getPathExp().toString();

		// check for dupes...
		for (Ordering o : clientEntity.getOrderings()) {
			if (sort.equals(o.getSortSpecString())) {
				return;
			}
		}

		if (direction == null) {
			direction = ASC;
		} else {
			checkInvalidDirection(direction);
		}

		SortOrder so = direction.equals(ASC) ? SortOrder.ASCENDING : SortOrder.DESCENDING;

		clientEntity.getOrderings().add(new Ordering(sort, so));
	}

	void processSorter(ClientEntity<?> clientEntity, String sort) {
		JsonNode root = jsonParser.parseJSON(sort, new ObjectMapper());

		if (root != null) {
			processSorter(clientEntity, root);
		}
	}

	void processSorter(ClientEntity<?> clientEntity, JsonNode root) {
		for (JsonNode sortNode : root) {
			JsonNode propertyNode = sortNode.get(PROPERTY);
			if (propertyNode == null || !propertyNode.isTextual()) {

				// this is a hack for Sencha bug, passing us null sorters
				// per LF-189...
				// So allowing for lax property name checking as a result
				if (propertyNode != null && propertyNode.isNull()) {
					LOGGER.info("ignoring NULL sort property");
					continue;
				}

				throw new LinkRestException(Status.BAD_REQUEST, "Bad sort spec: " + root);
			}

			String property = propertyNode.asText();
			String direction = ASC;

			JsonNode directionNode = sortNode.get(DIRECTION);
			if (directionNode != null) {
				direction = directionNode.asText();
			}

			processSimpleSorter(clientEntity, property, direction);
		}
	}

	private static void checkInvalidDirection(String direction) {
		if (!(ASC.equals(direction) || DESC.equals(direction))) {
			throw new LinkRestException(Status.BAD_REQUEST, "Direction is invalid: " + direction);
		}
	}
}
