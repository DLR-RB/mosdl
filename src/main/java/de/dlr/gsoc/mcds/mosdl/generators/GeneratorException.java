// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.generators;

/**
 * Exception class for problems during generation of MO service specification representations.
 */
public class GeneratorException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @see Exception#Exception()
	 */
	public GeneratorException() {
	}

	/**
	 * @param message the detail message
	 * @see Exception#Exception(String)
	 */
	public GeneratorException(String message) {
		super(message);
	}

	/**
	 * @param message the detail message
	 * @param cause the cause
	 * @see Exception#Exception(java.lang.String, java.lang.Throwable)
	 */
	public GeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause the cause
	 * @see Exception#Exception(java.lang.Throwable)
	 */
	public GeneratorException(Throwable cause) {
		super(cause);
	}

}
