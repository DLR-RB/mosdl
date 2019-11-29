// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import java.io.File;
import java.net.URISyntaxException;
import javax.xml.bind.JAXBElement;
import org.ccsds.schema.serviceschema.AnyTypeReference;
import org.ccsds.schema.serviceschema.AreaType;
import org.ccsds.schema.serviceschema.CapabilitySetType;
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
		return new File(TestUtils.class.getResource(fileName).toURI());
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
		assertNull(loadedService.getDataTypes());
		assertTrue(loadedService.getErrors().isEmpty());
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
		assertFalse(loadedOp.isSupportInReplay());
		assertEquals(RequestOperationType.class, loadedOp.getClass());

		RequestOperationType loadedReqOp = (RequestOperationType) loadedOp;
		assertTrue(loadedReqOp.getErrors().isEmpty());
		assertNotNull(loadedReqOp.getMessages());

		AnyTypeReference loadedReqMsg = loadedReqOp.getMessages().getRequest();
		assertNotNull(loadedReqMsg);
		assertNull(loadedReqMsg.getComment());
		assertEquals(1, loadedReqMsg.getAny().size());
		assertEquals(JAXBElement.class, loadedReqMsg.getAny().get(0).getClass());

		JAXBElement loadedReqField = (JAXBElement) loadedReqMsg.getAny().get(0);
		assertNotNull(loadedReqField.getValue());
		assertEquals(NamedElementReferenceWithCommentType.class, loadedReqField.getValue().getClass());

		NamedElementReferenceWithCommentType loadedInField = (NamedElementReferenceWithCommentType) loadedReqField.getValue();
		assertEquals("in", loadedInField.getName());
		assertFalse(loadedInField.isCanBeNull());
		assertNull(loadedInField.getComment());

		TypeReference loadedInFieldType = loadedInField.getType();
		assertEquals("MAL", loadedInFieldType.getArea());
		assertNull(loadedInFieldType.getService());
		assertEquals("String", loadedInFieldType.getName());
		assertTrue(loadedInFieldType.isList());

		AnyTypeReference loadedRespMsg = loadedReqOp.getMessages().getResponse();
		assertNotNull(loadedRespMsg);
		assertNull(loadedRespMsg.getComment());
		assertEquals(1, loadedRespMsg.getAny().size());
		assertEquals(JAXBElement.class, loadedRespMsg.getAny().get(0).getClass());

		JAXBElement loadedRespField = (JAXBElement) loadedRespMsg.getAny().get(0);
		assertNotNull(loadedRespField.getValue());
		assertEquals(NamedElementReferenceWithCommentType.class, loadedRespField.getValue().getClass());

		NamedElementReferenceWithCommentType loadedOutField = (NamedElementReferenceWithCommentType) loadedRespField.getValue();
		assertEquals("out", loadedOutField.getName());
		assertTrue(loadedOutField.isCanBeNull());
		assertNull(loadedOutField.getComment());

		TypeReference loadedOutFieldType = loadedOutField.getType();
		assertEquals("MAL", loadedOutFieldType.getArea());
		assertNull(loadedOutFieldType.getService());
		assertEquals("Integer", loadedOutFieldType.getName());
		assertFalse(loadedOutFieldType.isList());
	}

}
