// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import de.dlr.gsoc.mcds.mosdl.generators.GeneratorException;
import de.dlr.gsoc.mcds.mosdl.generators.MosdlGenerator;
import de.dlr.gsoc.mcds.mosdl.loaders.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main class for command line interface to MOSDL compiler.
 */
public class CliMain {

	private static final Logger logger = LoggerFactory.getLogger(CliMain.class);
	private static final String APP_VERSION = CliMain.class.getPackage().getImplementationVersion();
	private static SystemExiter systemExiter = status -> System.exit(status);

	public static void main(String[] args) throws Exception {
		CommandLineOptions opts = CommandLineOptions.create()
				.description("Compiler for MOSDL - Mission Operations Service Description Language for CCSDS MO Services (version " + APP_VERSION + ")")
				.required("service-descr", String.class, "MO service description file (CCSDS MO XML or MOSDL service description language) or directory containing files in MOSDL service description language.")
				.optional("target-dir", String.class, ".", "Target directory for generated files.")
				.toggle("xml", "x", "If given MO XML service description file will be generated.")
				.toggle("mosdl", "m", "If given MOSDL service description file will be generated.")
				.toggle("xsd", "s", "If given an MO data structure XML Schema file will be generated (EXPERIMENTAL).")
				.toggle("create-body-types", "b", "If given specialized non-standard message body XSD types will be generated for operations (only applicable when generating XML Schema files).")
				.toggle("skip-validation", "sv", "If given try to recover from MOSDL files with errors and do not validate XML input and output files against the service schema. Useful for slightly malformed files.")
				.optional("doc-type", "t", "doc-type", MosdlGenerator.DocType.class, MosdlGenerator.DocType.BULK, "Type of documentation to create for MOSDL or XSD files (BULK: operation documentation will be generated in bulk; INLINE: operation documentation will be put in-line; SUPPRESS: all documentation is stripped). BULK and INLINE produce the same results for XSD files.")
				.build()
				.parse(args);

		switch (opts.getParseResult()) {
			case ERROR:
				systemExiter.exit(-1);
			case UNPARSED:
				systemExiter.exit(-2);
			case HELP_REQUESTED:
				systemExiter.exit(0);
			case SUCCESS:
				break;
			default:
				systemExiter.exit(-3);
		}

		String serviceFile = opts.get("service-descr");
		String targetDirectory = opts.get("target-dir");
		boolean createXml = opts.get("xml");
		boolean createMosdl = opts.get("mosdl");
		boolean createXsd = opts.get("xsd");
		boolean isCreateXsdBodyTypes = opts.get("create-body-types");
		boolean isSkipValidation = opts.get("skip-validation");
		MosdlGenerator.DocType docType = opts.get("doc-type");

		Runner runner = new MosdlRunner(isSkipValidation, createXml, createMosdl, createXsd, isCreateXsdBodyTypes, docType);
		try {
			runner.execute(new File(targetDirectory), new File(serviceFile));
			logger.info("Compilation of '{}' successful.", serviceFile);
		} catch (LoaderException lEx) {
			logger.error("Specification load error: {}", lEx.getLocalizedMessage());
		} catch (GeneratorException gEx) {
			logger.error("Generator error: {}", gEx.getLocalizedMessage());
		} catch (Exception ex) {
			// stack trace is only needed for unexpected exceptions
			logger.error("Unexpected failure.", ex);
		}
	}

	/**
	 * Interface for intercepting calls to `System.exit()`.
	 * <p>
	 * This is mainly useful for integration testing of the CLI.
	 */
	@FunctionalInterface
	public interface SystemExiter {
		public void exit(int status);
	}

	public static SystemExiter getSystemExiter() {
		return systemExiter;
	}

	public static void setSystemExiter(final SystemExiter exiter) {
		systemExiter = exiter;
	}
}
