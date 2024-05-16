// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.generators;

import de.dlr.gsoc.mcds.mosdl.loaders.XmlSpecLoader;
import java.io.File;
import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Marshaller.Listener;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.ccsds.schema.serviceschema.AreaType;
import org.ccsds.schema.serviceschema.InvokeOperationType;
import org.ccsds.schema.serviceschema.ProgressOperationType;
import org.ccsds.schema.serviceschema.PubSubOperationType;
import org.ccsds.schema.serviceschema.RequestOperationType;
import org.ccsds.schema.serviceschema.ServiceType;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.SubmitOperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Class for generating an MO XML specification file.
 */
public class XmlGenerator extends Generator {

	private static final Logger logger = LoggerFactory.getLogger(XmlGenerator.class);

	private static final String SERVICE_SCHEMA_RESOURCE = "/COMSchema.xsd";
	private static final String XML_SPEC_FILE_ENDING = ".xml";
	private static final String DEFAULT_FILENAME = "spec";
	private final boolean isSkipValidation;

	/**
	 * Creates a new XML generator.
	 *
	 * @param isSkipValidation {@code true} if any validation of the generated XML file shall be
	 * skipped, {@code false} otherwise
	 */
	public XmlGenerator(boolean isSkipValidation) {
		this.isSkipValidation = isSkipValidation;
	}

	@Override
	public void generate(SpecificationType spec, File target) throws GeneratorException {
		File targetFile = target;
		if (target.isDirectory()) {
			String filename = DEFAULT_FILENAME;
			if (!spec.getArea().isEmpty()) {
				AreaType firstArea = spec.getArea().get(0);
				filename = firstArea.getName();
				if (!firstArea.getService().isEmpty()) {
					filename += firstArea.getService().get(0).getName();
				}
			}
			targetFile = new File(target, filename + XML_SPEC_FILE_ENDING);
		}
		logger.debug("Generating XML file '{}'. Skip validation of XML file: {}.", targetFile, isSkipValidation);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(SpecificationType.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			org.ccsds.schema.serviceschema.ObjectFactory serviceSchemaObjectFactory = new org.ccsds.schema.serviceschema.ObjectFactory();
			JAXBElement element = serviceSchemaObjectFactory.createSpecification(spec);
			if (!isSkipValidation) {
				SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				Schema schema = schemaFactory.newSchema(XmlSpecLoader.class.getResource(SERVICE_SCHEMA_RESOURCE));
				jaxbMarshaller.setSchema(schema);
			}
			jaxbMarshaller.setListener(new CleanupListener());
			jaxbMarshaller.marshal(element, targetFile);
			logger.debug("Finished generation of XML file '{}'.", targetFile);
		} catch (JAXBException | SAXException ex) {
			throw new GeneratorException(ex);
		}
	}

	/**
	 * This listener cleans up the model during marshalling.
	 * <p>
	 * Currently JAXB (or rather the XMLElementWrapper plugin) has a bug that generates invalid XML
	 * documents for an optional element that must contain at least one other element when no other
	 * element is present. In other words, in this case an empty list must not be encoded as an
	 * empty wrapper element but the wrapper element must be completely absent.
	 * <p>
	 * This class knows about the occurrences of this particular structure in the service schema and
	 * cleans up the model before feeding it to the marshaller.
	 * <p>
	 * Currently, only error lists exhibit this structure. They are used for area, service and all
	 * operation types (except SEND).
	 */
	private static class CleanupListener extends Listener {

		@Override
		public void beforeMarshal(Object source) {
			if (source instanceof AreaType && ((AreaType) source).getErrors().isEmpty()) {
				((AreaType) source).setErrors(null);
			} else if (source instanceof ServiceType && ((ServiceType) source).getErrors().isEmpty()) {
				((ServiceType) source).setErrors(null);
			} else if (source instanceof SubmitOperationType && ((SubmitOperationType) source).getErrors().isEmpty()) {
				((SubmitOperationType) source).setErrors(null);
			} else if (source instanceof RequestOperationType && ((RequestOperationType) source).getErrors().isEmpty()) {
				((RequestOperationType) source).setErrors(null);
			} else if (source instanceof InvokeOperationType && ((InvokeOperationType) source).getErrors().isEmpty()) {
				((InvokeOperationType) source).setErrors(null);
			} else if (source instanceof ProgressOperationType && ((ProgressOperationType) source).getErrors().isEmpty()) {
				((ProgressOperationType) source).setErrors(null);
			} else if (source instanceof PubSubOperationType && ((PubSubOperationType) source).getErrors().isEmpty()) {
				((PubSubOperationType) source).setErrors(null);
			}
		}
	}

}
