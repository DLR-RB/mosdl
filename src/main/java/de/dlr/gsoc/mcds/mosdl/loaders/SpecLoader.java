// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.loaders;

import java.io.File;
import org.ccsds.schema.serviceschema.SpecificationType;

/**
 * Interface for loaders of MO service specifications in different formats.
 * <p>
 * Usually a specification loader should be configured when creating it. At a later point in time
 * {@link #setInput(File...)} is called in order to set the input source the loader shall act upon.
 * A call to {@link #isLoadable()} gives the class user a hint whether the specification is
 * loadable. Finally, the specification is really loaded with {@link #load()}.
 */
public interface SpecLoader {

	/**
	 * Sets the input files and directories that comprise a single MO specification.
	 * <p>
	 * It is implementation-specific whether single or multiple files and/or directories are
	 * supported. No exception shall be thrown if {@code input} is unsupported.
	 *
	 * @param input the input files and/or directories
	 */
	void setInput(File... input);

	/**
	 * Gets the input files and/or directories that were set previously by
	 * {@link #setInput(File...)} and that will be used by this loader.
	 * <p>
	 * The return value of this method might differ from what was set, e.g. if the loader discards
	 * some entries right away.
	 *
	 * @return the previously set and used input files and/or directories
	 */
	File[] getInput();

	/**
	 * Determines whether the current specification loader is able to load a supplied specification.
	 * <p>
	 * This method does not need to perform in-depth checks or even perform a 'test' load. It shall
	 * just let the user know whether she can expect that the supplied specification can be loaded.
	 * For example, a file ending check would qualify.
	 *
	 * @return {@code true} if this loader can load a specification, {@code false} otherwise.
	 */
	boolean isLoadable();

	/**
	 * Instructs the loader to load an MO specification that has been assigned by
	 * {@link #setInput(File...)}.
	 * <p>
	 * @return the loaded MO specification
	 * @throws LoaderException thrown for any severe problems encountered during loading of the
	 * specification
	 */
	SpecificationType load() throws LoaderException;
}
