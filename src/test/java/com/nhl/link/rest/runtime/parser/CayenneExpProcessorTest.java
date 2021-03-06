package com.nhl.link.rest.runtime.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.cayenne.exp.Expression;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhl.link.rest.ClientEntity;
import com.nhl.link.rest.LinkRestException;
import com.nhl.link.rest.runtime.parser.CayenneExpProcessor;
import com.nhl.link.rest.runtime.parser.PathCache;
import com.nhl.link.rest.runtime.parser.RequestJsonParser;
import com.nhl.link.rest.unit.TestWithCayenneMapping;
import com.nhl.link.rest.unit.cayenne.E4;

public class CayenneExpProcessorTest extends TestWithCayenneMapping {

	private ClientEntity<E4> e4Descriptor;
	private CayenneExpProcessor processor;

	@Before
	public void setUp() {

		JsonFactory jsonFactory = new ObjectMapper().getJsonFactory();
		RequestJsonParser jsonParser = new RequestJsonParser(jsonFactory);

		PathCache pathCache = new PathCache();
		this.e4Descriptor = getClientEntity(E4.class);
		this.processor = new CayenneExpProcessor(jsonParser, pathCache);
	}

	@Test
	public void testProcess_NoParams() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cInt = 12345 and cVarchar = 'John Smith' and cBoolean = true\"}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cInt = 12345 and cVarchar = 'John Smith' and cBoolean = true"),
				e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_String() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cVarchar=$s\", \"params\":{\"s\":\"x\"}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cVarchar='x'"), e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Int() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cInt=$n\", \"params\":{\"n\":453}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cInt=453"), e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Float() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cDecimal=$n\", \"params\":{\"n\":4.4009}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cDecimal=4.4009"), e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Float_Negative() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cDecimal=$n\", \"params\":{\"n\":-4.4009}}");

		assertNotNull(e4Descriptor.getQualifier());

		// Cayenne parses 'fromString' as ASTNegate(ASTScalar), so to compare
		// apples to apples, let's convert it back to String.. not an ideal
		// comparison, but a good approximation
		assertEquals("cDecimal = -4.4009", e4Descriptor.getQualifier().toString());
	}

	@Test
	public void testProcess_Params_Boolean_True() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cBoolean=$b\", \"params\":{\"b\": true}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cBoolean=true"), e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Boolean_False() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cBoolean=$b\", \"params\":{\"b\": false}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cBoolean=false"), e4Descriptor.getQualifier());
	}

	@Test(expected = LinkRestException.class)
	public void testProcess_Params_InvalidPath() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"invalid/path=$b\", \"params\":{\"b\": false}}");
	}

	@Test
	public void testProcess_Params_Null() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cBoolean=$b\", \"params\":{\"b\": null}}");

		assertNotNull(e4Descriptor.getQualifier());
		assertEquals(Expression.fromString("cBoolean=null"), e4Descriptor.getQualifier());
	}

	@Test(expected = LinkRestException.class)
	public void testProcess_Params_Date_NonISO() {
		processor.process(e4Descriptor, "{\"exp\" : \"cTimestamp=$d\", \"params\":{\"d\": \"2014:02:03\"}}");
	}

	@Test
	public void testProcess_Params_Date_Local_TZ() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cTimestamp=$d\", \"params\":{\"d\": \"2014-02-03T14:06:35\"}}");

		assertNotNull(e4Descriptor.getQualifier());

		GregorianCalendar cal = new GregorianCalendar(2014, 1, 3, 14, 6, 35);
		cal.setTimeZone(TimeZone.getDefault());
		Date date = cal.getTime();

		Expression expected = Expression.fromString("cTimestamp=$d").expWithParameters(
				Collections.singletonMap("d", date));
		assertEquals(expected, e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Date_TZ_Zulu() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cTimestamp=$d\", \"params\":{\"d\": \"2014-02-03T22:06:35Z\"}}");

		assertNotNull(e4Descriptor.getQualifier());

		GregorianCalendar cal = new GregorianCalendar(2014, 1, 3, 14, 6, 35);
		cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		Date date = cal.getTime();

		Expression expected = Expression.fromString("cTimestamp=$d").expWithParameters(
				Collections.singletonMap("d", date));
		assertEquals(expected, e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Date_TZ_Zulu_DST() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cTimestamp=$d\", \"params\":{\"d\": \"2013-06-03\"}}");

		assertNotNull(e4Descriptor.getQualifier());

		GregorianCalendar cal = new GregorianCalendar(2013, 5, 3);
		cal.setTimeZone(TimeZone.getDefault());
		Date date = cal.getTime();

		Expression expected = Expression.fromString("cTimestamp=$d").expWithParameters(
				Collections.singletonMap("d", date));
		assertEquals(expected, e4Descriptor.getQualifier());
	}

	@Test
	public void testProcess_Params_Date_NoTime() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"cTimestamp=$d\", \"params\":{\"d\": \"2013-06-03T22:06:35Z\"}}");

		assertNotNull(e4Descriptor.getQualifier());

		GregorianCalendar cal = new GregorianCalendar(2013, 5, 3, 15, 6, 35);
		cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		Date date = cal.getTime();

		Expression expected = Expression.fromString("cTimestamp=$d").expWithParameters(
				Collections.singletonMap("d", date));
		assertEquals(expected, e4Descriptor.getQualifier());
	}
	
	@Test(expected = LinkRestException.class)
	public void testProcess_DisallowDBPath() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"db:id=$i\", \"params\":{\"i\": 5}}");
	}
	
	@Test
	public void testProcess_MatchByRootId() {

		assertNull(e4Descriptor.getQualifier());
		processor.process(e4Descriptor, "{\"exp\" : \"id=$i\", \"params\":{\"i\": 5}}");
		Expression expected = Expression.fromString("db:id=$i").expWithParameters(
				Collections.singletonMap("i", 5));
		assertEquals(expected, e4Descriptor.getQualifier());
	}
}
