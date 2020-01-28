// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import de.dlr.gsoc.mcds.mosdl.generators.Generator;
import de.dlr.gsoc.mcds.mosdl.generators.MosdlGenerator;
import de.dlr.gsoc.mcds.mosdl.generators.XmlGenerator;
import de.dlr.gsoc.mcds.mosdl.generators.XsdGenerator;
import de.dlr.gsoc.mcds.mosdl.loaders.MosdlSpecLoader;
import de.dlr.gsoc.mcds.mosdl.loaders.SpecLoader;
import de.dlr.gsoc.mcds.mosdl.loaders.XmlSpecLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that ties together the following MO specifications loaders:
 * <ul>
 * <li>{@link XmlSpecLoader}
 * <li>{@link MosdlSpecLoader}
 * </ul>
 * with the following generators:
 * <ul>
 * <li>{@link XmlGenerator}
 * <li>{@link MosdlGenerator}
 * </ul>
 */
public class MosdlRunner extends Runner {

	private final boolean isSkipValidation;
	private final boolean createXml;
	private final boolean createMosdl;
	private final boolean createXsd;
	private final MosdlGenerator.DocType docType;

	/**
	 * Creates and configures a new runner for loading and transforming an MO specification.
	 *
	 * @param isSkipValidation {@code true} for skipping MO XML input and output validations,
	 * {@code false} otherwise
	 * @param createXml {@code true} if MO XML files shall be generated, {@code false} otherwise
	 * @param createMosdl {@code true} if MOSDL files shall be generated, {@code false} otherwise
	 * @param createXsd {@code true} if MO data structure XSD files shall be generated, {@code false} otherwise
	 * @param docType only applicable if {@code createMosdl} is {@code true}. Determines the type of
	 * documentation to generate for MOSDL files.
	 */
	public MosdlRunner(boolean isSkipValidation, boolean createXml, boolean createMosdl, boolean createXsd, MosdlGenerator.DocType docType) {
		this.isSkipValidation = isSkipValidation;
		this.createXml = createXml;
		this.createMosdl = createMosdl;
		this.createXsd = createXsd;
		this.docType = docType;
	}

	@Override
	public List<SpecLoader> createSpecLoaders() {
		List<SpecLoader> specLoaders = Arrays.asList(
				new XmlSpecLoader(isSkipValidation),
				new MosdlSpecLoader(isSkipValidation));
		return specLoaders;
	}

	@Override
	public List<Generator> createGenerators() {
		List<Generator> generators = new ArrayList<>();

		if (createXml) {
			generators.add(new XmlGenerator(isSkipValidation));
		}
		if (createMosdl) {
			generators.add(new MosdlGenerator(docType));
		}
		if (createXsd) {
			generators.add(new XsdGenerator());
		}
		return generators;
	}

}
