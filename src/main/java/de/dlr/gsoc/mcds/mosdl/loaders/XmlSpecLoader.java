// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.loaders;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Specification loader that loads an MO specification from a single MO XML file.
 * <p>
 * The XML file has to conform to the extended MO service specification schema available at
 * https://sanaregistry.org/r/moschemas Validation can be disabled when creating the loader instance
 * in order to try loading specification files that are not completely valid. However, no guarantees
 * are made in this case.
 */
public class XmlSpecLoader implements SpecLoader {

	private static final Logger logger = LoggerFactory.getLogger(XmlSpecLoader.class);

	private static final String XML_SPEC_FILE_ENDING = ".xml";
	private static final String SERVICE_SCHEMA_RESOURCE = "/COMSchema.xsd";

	private final boolean isSkipValidation;
	private File file;

	/**
	 * Creates a new XML specification loader.
	 * <p>
	 * @param isSkipValidation {@code true} to disable schema validation, {@code false} otherwise
	 */
	public XmlSpecLoader(final boolean isSkipValidation) {
		this.isSkipValidation = isSkipValidation;
	}

	/**
	 * Sets the input XML file for loading an MO service specification from.
	 * <p>
	 * @param input the MO service description XML file to load. Neither directories nor multiple
	 * files are supported.
	 */
	@Override
	public void setInput(File... input) {
		if (input.length > 1) {
			logger.warn("The XML loader only supports loading from a single XML file. The first supplied file will be used.");
		}
		this.file = input[0];
	}

	@Override
	public File[] getInput() {
		return new File[]{file};
	}

	@Override
	public boolean isLoadable() {
		return null != file && file.isFile() && file.getPath().toLowerCase().endsWith(XML_SPEC_FILE_ENDING);
	}

	@Override
	public SpecificationType load() throws LoaderException {
		logger.debug("Loading specification from XML file '{}'. Skip validation: {}.", file, isSkipValidation);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(SpecificationType.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			if (!isSkipValidation) {
				SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				Schema schema = schemaFactory.newSchema(XmlSpecLoader.class.getResource(SERVICE_SCHEMA_RESOURCE));
				jaxbUnmarshaller.setSchema(schema);
			}
			SpecificationType spec = (SpecificationType) ((JAXBElement) jaxbUnmarshaller.unmarshal(file)).getValue();
			logger.debug("Loaded specification from XML file '{}'.", file);
			return spec;
		} catch (JAXBException | SAXException ex) {
			throw new LoaderException(ex);
		}
	}
}
