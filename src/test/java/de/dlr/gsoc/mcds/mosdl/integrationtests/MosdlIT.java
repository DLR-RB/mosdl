// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.integrationtests;

import de.dlr.gsoc.mcds.mosdl.MosdlRunner;
import de.dlr.gsoc.mcds.mosdl.Runner;
import de.dlr.gsoc.mcds.mosdl.TestUtils;
import static de.dlr.gsoc.mcds.mosdl.TestUtils.assertXmlEquals;
import de.dlr.gsoc.mcds.mosdl.generators.MosdlGenerator;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MosdlIT {

	private static final Logger logger = LoggerFactory.getLogger(MosdlIT.class);
	private static final String[] SIMPLE_TEST_CASES = {"MinimalTest",
		"AreaTest", "AreaTestPlain", "AreaTestOnlyNumber", "AreaTestOnlyVersion", "AreaTestFull",
		"ServiceTest", "CapSetTest", "OpTest", "OpDocInlineTest", "OpDocBulkTest",
		"OpSendTest", "OpSubmitTest", "OpRequestTest", "OpInvokeTest", "OpProgressTest", "OpPubsubTest",
		"CompositeTest", "CompositeAbstractTest", "EnumTest", "ErrorTest",
		"ServiceCompositeTest", "ServiceCompositeAbstractTest", "ServiceEnumTest", "ServiceErrorTest",
		"CommentTest", "ImportTest", "ImportComplexTest",
		"FundamentalTest", "AttributeTest"};
	private static final String[] COMPLEX_TEST_CASES = {"VerySimpleService", "ComplexService"};

	// do not change method signature without changing MethodSource annotations on test methods
	static Stream<String> simpleTestCaseProvider() {
		return Stream.of(SIMPLE_TEST_CASES);
	}

	// do not change method signature without changing MethodSource annotations on test methods
	static Stream<String> allTestCaseProvider() {
		return Stream.concat(Stream.of(SIMPLE_TEST_CASES), Stream.of("VerySimpleService", "ComplexService"));
	}

	@ParameterizedTest()
	@MethodSource("simpleTestCaseProvider")
	void mosdlToXmlTest(String input, @TempDir File targetDirectory) throws Exception {
		logger.info("MOSDL to XML Test: '{}'", input);
		String inputFilePath = "/mosdl/" + input + ".mosdl";
		String outputFileName = input + ".xml";
		String expectedFilePath = "/xml/" + outputFileName;

		File inputFile = TestUtils.getResource(inputFilePath);
		File outputFile = new File(targetDirectory, outputFileName);
		File expectedFile = TestUtils.getResource(expectedFilePath);

		Runner runner = new MosdlRunner(false, true, false, false, null);
		runner.execute(outputFile, inputFile);

		assertXmlEquals(expectedFile, outputFile);
	}

	@Test
	void complexMosdlToXmlTest(@TempDir File targetDirectory) throws Exception {
		String inputFilePath1 = "/mosdl/VerySimpleService.mosdl";
		String inputFilePath2 = "/mosdl/ComplexService.mosdl";
		String outputFileName = "MosdlDocExample.xml";
		String expectedFilePath = "/xml/" + outputFileName;

		File inputFile1 = TestUtils.getResource(inputFilePath1);
		File inputFile2 = TestUtils.getResource(inputFilePath2);
		File outputFile = new File(targetDirectory, outputFileName);
		File expectedFile = TestUtils.getResource(expectedFilePath);

		Runner runner = new MosdlRunner(false, true, false, false, null);
		runner.execute(outputFile, inputFile1, inputFile2);

		assertXmlEquals(expectedFile, outputFile);
	}

	@ParameterizedTest()
	@MethodSource("simpleTestCaseProvider")
	void xmlMosdlXmlRoundtripBulkDocTest(String input, @TempDir File mosdlTargetDirectory, @TempDir File xmlTargetDirectory) throws Exception {
		logger.info("XML-MOSDL-XML Roundtrip Test (Bulk doc): '{}'", input);
		xmlMosdlXmlRoundtripTest(input, mosdlTargetDirectory, xmlTargetDirectory, MosdlGenerator.DocType.BULK);
	}

	@ParameterizedTest()
	@MethodSource("simpleTestCaseProvider")
	void xmlMosdlXmlRoundtripInlineDocTest(String input, @TempDir File mosdlTargetDirectory, @TempDir File xmlTargetDirectory) throws Exception {
		logger.info("XML-MOSDL-XML Roundtrip Test (Inline doc): '{}'", input);
		xmlMosdlXmlRoundtripTest(input, mosdlTargetDirectory, xmlTargetDirectory, MosdlGenerator.DocType.INLINE);
	}

	private void xmlMosdlXmlRoundtripTest(String input, File mosdlTargetDirectory, File xmlTargetDirectory, MosdlGenerator.DocType docType) throws Exception {
		String xmlFileName = input + ".xml";
		String inputFilePath = "/xml/" + xmlFileName;

		File inputFile = TestUtils.getResource(inputFilePath);
		File outputFile = new File(xmlTargetDirectory, xmlFileName);

		Runner xmlToMosdlRunner = new MosdlRunner(false, false, true, false, docType);
		xmlToMosdlRunner.execute(mosdlTargetDirectory, inputFile);

		Runner mosdlToXmlRunner = new MosdlRunner(false, true, false, false, null);
		mosdlToXmlRunner.execute(outputFile, mosdlTargetDirectory);

		assertXmlEquals(inputFile, outputFile);
	}

	@ParameterizedTest()
	@MethodSource("allTestCaseProvider")
	void mosdlToXsdTest(String input, @TempDir File targetDirectory) throws Exception {
		logger.info("MOSDL to XSD Test: '{}'", input);
		String inputFilePath = "/mosdl/" + input + ".mosdl";
		String expectedDirectoryPath = "/xsd/" + input;

		File inputFile = TestUtils.getResource(inputFilePath);
		File outputDirectory = new File(targetDirectory, input);
		outputDirectory.mkdirs();
		File expectedDirectory = TestUtils.getResource(expectedDirectoryPath);

		Runner runner = new MosdlRunner(false, false, false, true, null);
		runner.execute(outputDirectory, inputFile);

		File[] expectedFiles = null == expectedDirectory ? new File[]{} : expectedDirectory.listFiles();
		File[] outputFiles = outputDirectory.listFiles();
		assertEquals(expectedFiles.length, outputFiles.length);

		Arrays.sort(expectedFiles);
		Arrays.sort(outputFiles);
		for (int i = 0; i < expectedFiles.length; i++) {
			assertXmlEquals(expectedFiles[i], outputFiles[i]);
		}
	}

}
