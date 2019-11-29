// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import de.dlr.gsoc.mcds.mosdl.generators.Generator;
import de.dlr.gsoc.mcds.mosdl.generators.GeneratorException;
import de.dlr.gsoc.mcds.mosdl.loaders.LoaderException;
import de.dlr.gsoc.mcds.mosdl.loaders.SpecLoader;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.ccsds.schema.serviceschema.SpecificationType;

/**
 * Class tying together MO specification loaders and generators.
 * <p>
 * This is the main class to be used to load an MO service specification using an appropriate
 * service specification loader and to generate other representations of the service specification
 * using generators.
 * <p>
 * Implementations of {@link #createSpecLoaders()} should create all available specification
 * loaders. Files are loaded with the first specification loader for which
 * {@link SpecLoader#isLoadable()} returns {@code true}. Afterwards, all generators returned by an
 * implementation of {@link #createGenerators()} are used to generate other representations of the
 * service specification.
 */
public abstract class Runner {

	/**
	 * Creates possible MO specification loaders responsible for loading specifications from
	 * different sources.
	 *
	 * @return a list of possible MO specification loaders
	 */
	protected abstract List<SpecLoader> createSpecLoaders();

	/**
	 * Creates generators in order to transform an MO specification to a different representation.
	 *
	 * @return a list of generators that generate different representations of an MO specification
	 */
	protected abstract List<Generator> createGenerators();

	/**
	 * Loads an MO service specification using an appropriate loader and generates all possible
	 * representations.
	 *
	 * @param targetDirectory the target directory where generators shall put their generated
	 * service representations
	 * @param input the input files or directories making up a single MO service specification. The
	 * meaning of this parameter depends on the concrete service loader.
	 * @throws LoaderException thrown if a supplied specification cannot be loaded
	 * @throws GeneratorException thrown if a generator encounters a severe problem. Subsequent
	 * generators are not run.
	 */
	public void execute(File targetDirectory, File... input) throws LoaderException, GeneratorException {
		List<SpecLoader> specLoaders = createSpecLoaders();
		specLoaders.forEach(loader -> loader.setInput(input));
		Optional<SpecLoader> loader = specLoaders.stream().filter(SpecLoader::isLoadable).findFirst();
		if (!loader.isPresent()) {
			throw new LoaderException("No loader found for supplied service description.");
		}
		SpecificationType spec = loader.get().load();

		List<Generator> generators = createGenerators();
		for (Generator generator : generators) {
			generator.generate(spec, targetDirectory);
		}
	}
}
