package com.nhl.link.rest.runtime.parser;

import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhl.link.rest.ClientEntity;
import com.nhl.link.rest.LinkRestException;

public class ExcludeProcessor {

	private RequestJsonParser jsonParser;

	ExcludeProcessor(RequestJsonParser jsonParser) {
		this.jsonParser = jsonParser;
	}

	/**
	 * Sanity check. We don't want to get a stack overflow.
	 */
	static void checkTooLong(String path) {
		if (path != null && path.length() > PathConstants.MAX_PATH_LENGTH) {
			throw new LinkRestException(Status.BAD_REQUEST, "Include/exclude path too long: " + path);
		}
	}

	void process(ClientEntity<?> clientEntity, List<String> excludes) {
		for (String exclude : excludes) {
			if (exclude.startsWith("[")) {
				processExcludeArray(clientEntity, exclude);
			} else {
				processExcludePath(clientEntity, exclude);
			}
		}
	}

	private void processExcludeArray(ClientEntity<?> clientEntity, String exclude) {
		JsonNode root = jsonParser.parseJSON(exclude, new ObjectMapper());

		if (root != null && root.isArray()) {

			for (JsonNode child : root) {
				if (child.isTextual()) {
					processExcludePath(clientEntity, child.asText());
				} else {
					throw new LinkRestException(Status.BAD_REQUEST, "Bad exclude spec: " + child);
				}
			}
		}
	}

	void processExcludePath(ClientEntity<?> clientEntity, String path) {

		checkTooLong(path);
		int dot = path.indexOf(PathConstants.DOT);

		if (dot == 0) {
			throw new LinkRestException(Status.BAD_REQUEST, "Exclude starts with dot: " + path);
		}

		if (dot == path.length() - 1) {
			throw new LinkRestException(Status.BAD_REQUEST, "Exclude ends with dot: " + path);
		}

		String property = dot > 0 ? path.substring(0, dot) : path;
		ObjAttribute attribute = (ObjAttribute) clientEntity.getEntity().getAttribute(property);
		if (attribute != null) {

			if (dot > 0) {
				throw new LinkRestException(Status.BAD_REQUEST, "Invalid exclude path: " + path);
			}

			clientEntity.getAttributes().remove(property);
			return;
		}

		ObjRelationship relationship = (ObjRelationship) clientEntity.getEntity().getRelationship(property);
		if (relationship != null) {

			ClientEntity<?> relatedFilter = clientEntity.getRelationships().get(property);
			if (relatedFilter == null) {
				// valid path, but not included... ignoring
				return;
			}

			if (dot > 0) {
				processExcludePath(relatedFilter, path.substring(dot + 1));
			}
			return;
		}

		// this is an entity id and it's excluded explicitly
		if (property.equals(PathConstants.ID_PK_ATTRIBUTE)) {
			clientEntity.setIdIncluded(false);
			return;
		}

		throw new LinkRestException(Status.BAD_REQUEST, "Invalid include path: " + path);
	}

}
