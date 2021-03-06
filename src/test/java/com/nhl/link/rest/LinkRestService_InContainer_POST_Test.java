package com.nhl.link.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.junit.Before;
import org.junit.Test;

import com.nhl.link.rest.unit.JerseyTestOnDerby;
import com.nhl.link.rest.unit.cayenne.E3;
import com.nhl.link.rest.unit.cayenne.E4;

public class LinkRestService_InContainer_POST_Test extends JerseyTestOnDerby {

	@Before
	public void before() {
		runtime.newContext().performGenericQuery(new EJBQLQuery("delete from E4"));
		runtime.newContext().performGenericQuery(new EJBQLQuery("delete from E3"));
		runtime.newContext().performGenericQuery(new EJBQLQuery("delete from E2"));
		runtime.newContext().performGenericQuery(new EJBQLQuery("delete from E5"));
	}

	@Test
	public void testPost() throws WebApplicationException, IOException {

		ObjectContext context = runtime.newContext();

		Response response1 = target("/lr").request().post(
				Entity.entity("{\"cVarchar\":\"zzz\"}", MediaType.APPLICATION_JSON));
		assertEquals(Status.OK.getStatusCode(), response1.getStatus());

		E4 e41 = (E4) Cayenne.objectForQuery(context, new SelectQuery<E4>(E4.class));
		assertEquals("zzz", e41.getCVarchar());
		int id1 = Cayenne.intPKForObject(e41);

		assertEquals("{\"success\":true,\"data\":[{\"id\":" + id1
				+ ",\"cBoolean\":null,\"cDate\":null,\"cDecimal\":null,\"cInt\":null,"
				+ "\"cTime\":null,\"cTimestamp\":null,\"cVarchar\":\"zzz\"}],\"total\":1}",
				response1.readEntity(String.class));

		Response response2 = target("/lr").request().post(
				Entity.entity("{\"cVarchar\":\"TTTT\"}", MediaType.APPLICATION_JSON));

		assertEquals(Status.OK.getStatusCode(), response2.getStatus());

		List<E4> e4s = context.select(new SelectQuery<E4>(E4.class));
		assertEquals(2, e4s.size());
		assertTrue(e4s.remove(e41));

		E4 e42 = e4s.get(0);
		assertEquals("TTTT", e42.getCVarchar());
		int id2 = Cayenne.intPKForObject(e42);

		assertEquals("{\"success\":true,\"data\":[{\"id\":" + id2
				+ ",\"cBoolean\":null,\"cDate\":null,\"cDecimal\":null,\"cInt\":null,"
				+ "\"cTime\":null,\"cTimestamp\":null,\"cVarchar\":\"TTTT\"}],\"total\":1}",
				response2.readEntity(String.class));
	}

	@Test
	public void testPost_ToOne() throws WebApplicationException, IOException {

		ObjectContext context = runtime.newContext();
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (1, 'xxx')"));
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (8, 'yyy')"));

		Response response1 = target("/lr/e3").request().post(
				Entity.entity("{\"e2_id\":8,\"name\":\"MM\"}", MediaType.APPLICATION_JSON));

		assertEquals(Status.OK.getStatusCode(), response1.getStatus());

		E3 e3 = (E3) Cayenne.objectForQuery(context, new SelectQuery<E3>(E3.class));
		int id = Cayenne.intPKForObject(e3);

		assertEquals("{\"success\":true,\"data\":[{\"id\":" + id
				+ ",\"name\":\"MM\",\"phoneNumber\":null}],\"total\":1}", response1.readEntity(String.class));

		runtime.newContext().invalidateObjects(e3);
		assertEquals("MM", e3.getName());
		assertEquals(8, Cayenne.intPKForObject(e3.getE2()));
	}

	@Test
	public void testPost_ToOne_Null() throws WebApplicationException, IOException {

		ObjectContext context = runtime.newContext();
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (1, 'xxx')"));
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (8, 'yyy')"));

		Response response1 = target("/lr/e3").request().post(
				Entity.entity("{\"e2_id\":null,\"name\":\"MM\"}", MediaType.APPLICATION_JSON));

		assertEquals(Status.OK.getStatusCode(), response1.getStatus());

		E3 e3 = (E3) Cayenne.objectForQuery(context, new SelectQuery<E3>(E3.class));
		int id = Cayenne.intPKForObject(e3);

		assertEquals("{\"success\":true,\"data\":[{\"id\":" + id
				+ ",\"name\":\"MM\",\"phoneNumber\":null}],\"total\":1}", response1.readEntity(String.class));

		runtime.newContext().invalidateObjects(e3);
		assertEquals("MM", e3.getName());
		assertNull(e3.getE2());
	}

	@Test
	public void testPost_ToOne_BadFK() throws WebApplicationException, IOException {

		ObjectContext context = runtime.newContext();
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (1, 'xxx')"));
		context.performGenericQuery(new SQLTemplate(E4.class, "INSERT INTO utest.e2 (id, name) values (8, 'yyy')"));

		Response response1 = target("/lr/e3").request().post(
				Entity.entity("{\"e2_id\":15,\"name\":\"MM\"}", MediaType.APPLICATION_JSON));

		assertEquals(Status.NOT_FOUND.getStatusCode(), response1.getStatus());

		assertEquals(0, context.select(new SelectQuery<E3>(E3.class)).size());
	}
}
