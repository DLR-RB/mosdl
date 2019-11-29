// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.loaders;

/**
 * Exception class for problems during loading of MO service specifications.
 */
public class LoaderException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @see Exception#Exception()
	 */
	public LoaderException() {
	}

	/**
	 * @param message the detail message
	 * @see Exception#Exception(String)
	 */
	public LoaderException(String message) {
		super(message);
	}

	/**
	 * @param message the detail message
	 * @param cause the cause
	 * @see Exception#Exception(java.lang.String, java.lang.Throwable)
	 */
	public LoaderException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause the cause
	 * @see Exception#Exception(java.lang.Throwable)
	 */
	public LoaderException(Throwable cause) {
		super(cause);
	}

}
