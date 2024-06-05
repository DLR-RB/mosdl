// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.ccsds.schema.serviceschema.AreaType;
import org.ccsds.schema.serviceschema.CapabilitySetType;
import org.ccsds.schema.serviceschema.MessageBodyType;
import org.ccsds.schema.serviceschema.NamedElementReferenceWithCommentType;
import org.ccsds.schema.serviceschema.OperationType;
import org.ccsds.schema.serviceschema.RequestOperationType;
import org.ccsds.schema.serviceschema.ServiceType;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.TypeReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

public class TestUtils {

	private TestUtils() {
		// utility class - not meant to be instantiated
	}

	public static File getResource(String fileName) throws URISyntaxException {
		URL url = TestUtils.class.getResource(fileName);
		if (null == url) {
			return null;
		}
		return new File(url.toURI());
	}

	public static void assertXmlEquals(File expected, File actual) {
		Diff xmlDiff = DiffBuilder
				.compare(Input.fromFile(expected))
				.withTest(Input.from(actual))
				.ignoreComments()
				.normalizeWhitespace()
				.checkForSimilar()
				.build();
		assertFalse(xmlDiff.hasDifferences(), () -> xmlDiff.toString());
	}

	public static void assertEqualsMinimalTestSpec(SpecificationType loadedSpec) {
		assertNull(loadedSpec.getComment());
		assertEquals(1, loadedSpec.getArea().size());

		AreaType loadedArea = loadedSpec.getArea().get(0);
		assertEquals("TestArea", loadedArea.getName());
		assertEquals(4711, loadedArea.getNumber());
		assertEquals(1, loadedArea.getVersion());
		assertNull(loadedArea.getDataTypes());
		assertTrue(loadedArea.getErrors().isEmpty());
		assertNull(loadedArea.getComment());
		assertEquals(1, loadedArea.getService().size());

		ServiceType loadedService = loadedArea.getService().get(0);
		assertEquals("TestSvc", loadedService.getName());
		assertEquals(1, loadedService.getNumber());
		assertNull(loadedService.getComment());
		assertEquals(1, loadedService.getCapabilitySet().size());

		CapabilitySetType loadedCs = loadedService.getCapabilitySet().get(0);
		assertEquals(1, loadedCs.getNumber());
		assertNull(loadedCs.getComment());
		assertEquals(1, loadedCs.getSendIPOrSubmitIPOrRequestIP().size());

		OperationType loadedOp = loadedCs.getSendIPOrSubmitIPOrRequestIP().get(0);
		assertEquals("testRequest", loadedOp.getName());
		assertEquals(1, loadedOp.getNumber());
		assertNull(loadedOp.getComment());
		assertEquals(RequestOperationType.class, loadedOp.getClass());

		RequestOperationType loadedReqOp = (RequestOperationType) loadedOp;
		assertTrue(loadedReqOp.getErrors().isEmpty());
		assertNotNull(loadedReqOp.getMessages());

		MessageBodyType loadedReqMsg = loadedReqOp.getMessages().getRequest();
		assertNotNull(loadedReqMsg);
		assertNull(loadedReqMsg.getComment());
		assertEquals(1, loadedReqMsg.getField().size());

		NamedElementReferenceWithCommentType loadedInField = loadedReqMsg.getField().get(0);
		assertNotNull(loadedInField);
		assertEquals("in", loadedInField.getName());
		assertFalse(loadedInField.isCanBeNull());
		assertNull(loadedInField.getComment());

		TypeReference loadedInFieldType = loadedInField.getType();
		assertEquals("MAL", loadedInFieldType.getArea());
		assertEquals("String", loadedInFieldType.getName());
		assertTrue(loadedInFieldType.isList());

		MessageBodyType loadedRespMsg = loadedReqOp.getMessages().getResponse();
		assertNotNull(loadedRespMsg);
		assertNull(loadedRespMsg.getComment());
		assertEquals(1, loadedRespMsg.getField().size());

		NamedElementReferenceWithCommentType loadedOutField = loadedRespMsg.getField().get(0);
		assertNotNull(loadedOutField);
		assertEquals("out", loadedOutField.getName());
		assertTrue(loadedOutField.isCanBeNull());
		assertNull(loadedOutField.getComment());

		TypeReference loadedOutFieldType = loadedOutField.getType();
		assertEquals("MAL", loadedOutFieldType.getArea());
		assertEquals("Integer", loadedOutFieldType.getName());
		assertFalse(loadedOutFieldType.isList());
	}

}
