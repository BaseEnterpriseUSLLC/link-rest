package com.nhl.link.rest.runtime.encoder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.nhl.link.rest.ClientEntity;
import com.nhl.link.rest.DataResponse;
import com.nhl.link.rest.encoder.EncoderFilter;
import com.nhl.link.rest.runtime.encoder.AttributeEncoderFactory;
import com.nhl.link.rest.runtime.encoder.EncoderService;
import com.nhl.link.rest.runtime.encoder.IAttributeEncoderFactory;
import com.nhl.link.rest.runtime.encoder.IStringConverterFactory;
import com.nhl.link.rest.runtime.jackson.JacksonService;
import com.nhl.link.rest.runtime.meta.DataMapBuilder;
import com.nhl.link.rest.runtime.semantics.RelationshipMapper;
import com.nhl.link.rest.unit.pojo.model.P1;
import com.nhl.link.rest.unit.pojo.model.P2;
import com.nhl.link.rest.unit.pojo.model.P3;
import com.nhl.link.rest.unit.pojo.model.P4;
import com.nhl.link.rest.unit.pojo.model.P6;

public class EncoderService_Pojo_Test {

	private EncoderService encoderService;
	private List<EncoderFilter> filters;
	private DataMap dataMap;

	@Before
	public void setUp() {

		this.dataMap = DataMapBuilder.newBuilder("__").addEntities(P1.class, P2.class, P3.class, P4.class)
				.addEntity(P6.class).withId("stringId").toDataMap();
		this.filters = new ArrayList<>();

		IAttributeEncoderFactory attributeEncoderFactory = new AttributeEncoderFactory();
		IStringConverterFactory stringConverterFactory = mock(IStringConverterFactory.class);

		this.encoderService = new EncoderService(this.filters, attributeEncoderFactory, stringConverterFactory,
				new RelationshipMapper());
	}

	@Test
	public void testEncode_SimplePojo_noId() throws IOException {
		ClientEntity<P1> descriptor = getClientEntity(P1.class);
		descriptor.getAttributes().add("name");

		DataResponse<P1> builder = DataResponse.forType(P1.class).withClientEntity(descriptor);
		encoderService.makeEncoder(builder);

		P1 p1 = new P1();
		p1.setName("XYZ");
		assertEquals("[{\"name\":\"XYZ\"}]", toJson(p1, builder));
	}

	@Test
	public void testEncode_SimplePojo_Id() throws IOException {

		P6 p6 = new P6();
		p6.setStringId("myid");
		p6.setIntProp(4);

		ClientEntity<P6> descriptor = getClientEntity(P6.class);
		descriptor.getAttributes().add("intProp");
		descriptor.setIdIncluded(true);
		DataResponse<P6> builder = DataResponse.forObjects(Collections.singletonList(p6)).withClientEntity(descriptor);
		encoderService.makeEncoder(builder);

		assertEquals("[{\"id\":\"myid\",\"intProp\":4}]", toJson(p6, builder));
	}

	private static String toJson(Object object, DataResponse<?> builder) throws IOException {

		// wrap in collection... root encoder expects a list...
		object = Collections.singletonList(object);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (JsonGenerator generator = new JacksonService().getJsonFactory()
				.createJsonGenerator(out, JsonEncoding.UTF8)) {
			builder.getEncoder().encode(null, object, generator);
		}

		return new String(out.toByteArray(), "UTF-8");
	}

	protected ObjEntity getEntity(Class<?> type) {
		return dataMap.getObjEntity(type);
	}

	protected <T> ClientEntity<T> getClientEntity(Class<T> type) {
		return new ClientEntity<T>(type, getEntity(type));
	}
}
