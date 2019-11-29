// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.loaders;

import de.dlr.gsoc.mcds.mosdl.TestUtils;
import static de.dlr.gsoc.mcds.mosdl.TestUtils.assertEqualsMinimalTestSpec;
import java.io.File;
import org.ccsds.schema.serviceschema.SpecificationType;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MosdlSpecLoaderTest {

	private static File testFile;
	private static File[] testFiles;

	@BeforeAll
	static void initAll() throws Exception {
		testFile = TestUtils.getResource("/mosdl/MinimalTest.mosdl");
		testFiles = new File[]{TestUtils.getResource("/mosdl/VerySimpleService.mosdl"),
			TestUtils.getResource("/mosdl/ComplexService.mosdl")};
	}

	@Test
	void testSetInput() {
		MosdlSpecLoader specLoader = new MosdlSpecLoader(false);
		specLoader.setInput(testFile);
		File[] expectedInput = new File[]{testFile};
		File[] actualInput = specLoader.getInput();
		assertArrayEquals(expectedInput, actualInput);
	}

	@Test
	void testSetMultipleInput() {
		MosdlSpecLoader specLoader = new MosdlSpecLoader(false);
		specLoader.setInput(testFiles);
		File[] actualInput = specLoader.getInput();
		assertArrayEquals(testFiles, actualInput);
	}

	@Test
	void testIsLoadable() {
		MosdlSpecLoader specLoader = new MosdlSpecLoader(false);
		specLoader.setInput(testFile);
		assertTrue(specLoader.isLoadable());
	}

	@Test
	void testIsNotLoadable() {
		MosdlSpecLoader specLoader = new MosdlSpecLoader(false);
		specLoader.setInput(new File("nonexistent.withwrongending"));
		assertFalse(specLoader.isLoadable());
	}

	@Test
	void testLoad() throws Exception {
		MosdlSpecLoader specLoader = new MosdlSpecLoader(false);
		specLoader.setInput(testFile);
		SpecificationType loadedSpec = specLoader.load();
		assertEqualsMinimalTestSpec(loadedSpec);
	}

}
