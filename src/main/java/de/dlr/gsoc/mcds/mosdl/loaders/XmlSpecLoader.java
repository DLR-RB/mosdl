// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.loaders;

import java.io.File;
import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.util.List;
import java.util.ListIterator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.ccsds.schema.serviceschema.AreaDataTypeList;
import org.ccsds.schema.serviceschema.CompositeType;
import org.ccsds.schema.serviceschema.ObjectType;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.TypeReference;
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
	private static final String SERVICE_SCHEMA_RESOURCE = "/ServiceSchema.xsd";

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
			jaxbUnmarshaller.setListener(new TransformListener());
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

	private static class TransformListener extends Unmarshaller.Listener {

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			// Try to recreate "isObjectRef" for type references from its name.
			if (target instanceof TypeReference && !((TypeReference) target).isObjectRef()) {
				TypeReference typeRef = (TypeReference) target;
				if (!typeRef.isObjectRef() && (typeRef.getName().startsWith("ObjectRef<") || typeRef.getName().startsWith("MAL::ObjectRef<")) && typeRef.getName().endsWith(">")) {
					typeRef.setObjectRef(Boolean.TRUE);
					int typeStart = typeRef.getName().indexOf("<");
					String innerTypeName = typeRef.getName().substring(typeStart + 1, typeRef.getName().length() - 1);
					typeRef.setName(innerTypeName);
				}
			}
			// Currently only composites that directly inherit from MAL::Object are recreated into Objects.
			if (target instanceof AreaDataTypeList) {
				List<Object> dataTypes = ((AreaDataTypeList) target).getFundamentalOrAttributeOrComposite();
				ListIterator<Object> iterator = dataTypes.listIterator();
				while (iterator.hasNext()) {
					Object dataType = iterator.next();
					if (dataType instanceof CompositeType && null != ((CompositeType) dataType).getExtends()) {
						CompositeType comp = (CompositeType) dataType;
						TypeReference ref = comp.getExtends().getType();
						boolean isObject = "MAL".equals(ref.getArea()) && "Object".equals(ref.getName());
						if (isObject) {
							ObjectType obj = new ObjectType();
							obj.setComment(comp.getComment());
							obj.setName(comp.getName());
							obj.setShortFormPart(comp.getShortFormPart());
							obj.getField().addAll(comp.getField());
							iterator.set(obj);
						}
					}
				}
			}
		}
	}
}
