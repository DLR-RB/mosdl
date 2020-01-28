// Copyright 2020 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.generators;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentProcessing;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.ccsds.schema.serviceschema.AreaType;
import org.ccsds.schema.serviceschema.AttributeType;
import org.ccsds.schema.serviceschema.CompositeType;
import org.ccsds.schema.serviceschema.EnumerationType;
import org.ccsds.schema.serviceschema.FundamentalType;
import org.ccsds.schema.serviceschema.NamedElementReferenceWithCommentType;
import org.ccsds.schema.serviceschema.ServiceType;

import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for generating MO XSD files describing data structures of an MO specification.
 * <p>
 * Each separate namespace is put in its own file, i.e. one file for each area and/or service that
 * define data structures.
 */
public class XsdGenerator extends Generator {

	private static final Logger logger = LoggerFactory.getLogger(XsdGenerator.class);

	private static final String XSD_FILE_ENDING = ".xsd";
	private static final String MAL_AREA_NAME = "MAL";
	// Mismatch between 5.2.1 and 3.7.3.2.1! urn:ccsds:schema:mo:malxml vs. http://www.ccsds.org/schema/malxml/MAL (latter seems to be more common and is used by NASA implementation)
	private static final String MALXML_NAMESPACE_BASE = "http://www.ccsds.org/schema/malxml/";
	private static final String MALXML_NAMESPACE = toNamespace(MAL_AREA_NAME, null);
	private static final QName XSD_MAL_ELEMENT = new QName(MALXML_NAMESPACE, "Element");
	private static final QName XSD_MAL_ATTRIBUTE = new QName(MALXML_NAMESPACE, "Attribute");
	private static final QName XSD_MAL_COMPOSITE = new QName(MALXML_NAMESPACE, "Composite");
	private static final Map<String, QName> ATTRIBUTE_XSD_MAPPING = new HashMap<>();
	private static final NamespaceMap NAMESPACE_MAP = new NamespaceMap();
	private static final Map<String, String> WRITE_OPTIONS = new HashMap<>();

	static {
		ATTRIBUTE_XSD_MAPPING.put("Blob", Constants.XSD_HEXBIN);
		ATTRIBUTE_XSD_MAPPING.put("Boolean", Constants.XSD_BOOLEAN);
		ATTRIBUTE_XSD_MAPPING.put("Duration", Constants.XSD_DURATION);
		ATTRIBUTE_XSD_MAPPING.put("Float", Constants.XSD_FLOAT);
		ATTRIBUTE_XSD_MAPPING.put("Double", Constants.XSD_DOUBLE);
		ATTRIBUTE_XSD_MAPPING.put("Identifier", Constants.XSD_STRING);
		ATTRIBUTE_XSD_MAPPING.put("Octet", Constants.XSD_BYTE);
		ATTRIBUTE_XSD_MAPPING.put("UOctet", Constants.XSD_UNSIGNEDBYTE);
		ATTRIBUTE_XSD_MAPPING.put("Short", Constants.XSD_SHORT);
		ATTRIBUTE_XSD_MAPPING.put("UShort", Constants.XSD_UNSIGNEDSHORT);
		ATTRIBUTE_XSD_MAPPING.put("Integer", Constants.XSD_INT);
		ATTRIBUTE_XSD_MAPPING.put("UInteger", Constants.XSD_UNSIGNEDINT);
		ATTRIBUTE_XSD_MAPPING.put("Long", Constants.XSD_LONG);
		ATTRIBUTE_XSD_MAPPING.put("ULong", Constants.XSD_UNSIGNEDLONG);
		ATTRIBUTE_XSD_MAPPING.put("String", Constants.XSD_STRING);
		ATTRIBUTE_XSD_MAPPING.put("Time", Constants.XSD_DATETIME);
		ATTRIBUTE_XSD_MAPPING.put("FineTime", Constants.XSD_DATETIME);
		ATTRIBUTE_XSD_MAPPING.put("URI", Constants.XSD_ANYURI);

		NAMESPACE_MAP.add("xs", Constants.URI_2001_SCHEMA_XSD);
		NAMESPACE_MAP.add("malxml", MALXML_NAMESPACE);

		WRITE_OPTIONS.put("omit-xml-declaration", "no");
		WRITE_OPTIONS.put("indent", "yes");
	}

	@Override
	public void generate(SpecificationType spec, File targetDirectory) throws GeneratorException {
		logger.debug("Generating XSD file(s) into directory '{}'.", targetDirectory);

		try {
			XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
			Map<String, XmlSchema> allSchemas = new LinkedHashMap<>();

			for (AreaType area : spec.getArea()) {
				if (null != area.getDataTypes()) {
					XmlSchema schema = addSchema(schemaCollection, area, null);
					allSchemas.put(area.getName(), schema);
					if (MAL_AREA_NAME.equals(area.getName())) {
						// XSD type and element for message body as defined in CCSDS 524.3-B-1, 3.7.3.2 need to be added explicitly.
						XmlSchemaType xsdType = addExtraMalBodyType(schema);
						addCorrespondingElement(schema, xsdType);
					}
					for (Object dataType : area.getDataTypes().getFundamentalOrAttributeOrComposite()) {
						addDataType(schema, dataType);
					}
				}
				for (ServiceType service : area.getService()) {
					if (null != service.getDataTypes()) {
						XmlSchema schema = addSchema(schemaCollection, area, service);
						allSchemas.put(area.getName() + service.getName(), schema);
						for (Object dataType : service.getDataTypes().getCompositeOrEnumeration()) {
							addDataType(schema, dataType);
						}
					}
				}
			}

			for (Map.Entry<String, XmlSchema> schemaEntry : allSchemas.entrySet()) {
				File targetFile = new File(targetDirectory, schemaEntry.getKey() + XSD_FILE_ENDING);
				try (OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile))) {
					schemaEntry.getValue().write(os, WRITE_OPTIONS);
				}
			}
		} catch (IOException ex) {
			throw new GeneratorException(ex);
		}
	}

	/**
	 * Add a new XML Schema to a schema collection.
	 *
	 * @param schemaCollection the schema collection to add the schema to
	 * @param area the MAL area containing the data structures to add, must not be {@code null}
	 * @param service the MAL service containing the data structures to add, may be {@code null}
	 * @return
	 */
	private static XmlSchema addSchema(XmlSchemaCollection schemaCollection, AreaType area, ServiceType service) {
		String namespace = toNamespace(area.getName(), null == service ? null : service.getName());
		XmlSchema schema = new XmlSchema(namespace, schemaCollection);
		schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
		schema.setAttributeFormDefault(XmlSchemaForm.QUALIFIED);
		schema.setInputEncoding(StandardCharsets.UTF_8.name());
		schema.setNamespaceContext(NAMESPACE_MAP);
		return schema;
	}

	/**
	 * Process a MAL data structure and add all necessary elements to the schema.
	 * <p>
	 * Necessary elements are:
	 * <ul>
	 * <li>a new schema type for the data structure,
	 * <li>if the structure is a concrete Composite, an Enumeration or an Attribute: a new schema
	 * type for the list of the data structure,
	 * <li>if the strucutre is a concrete Composite, an Enumeration or an Attribute: a new element
	 * for using the data structure as MAL message body element,
	 * <li>if the strucutre is a concrete Composite, an Enumeration or an Attribute: a new element
	 * for using a list of the data structure as MAL body element
	 * </ul>
	 *
	 * @param schema the schema to add all required elements to
	 * @param dataType the MAL data structure to process
	 */
	private static void addDataType(XmlSchema schema, Object dataType) {
		if (dataType instanceof CompositeType) {
			CompositeType composite = (CompositeType) dataType;
			boolean isAbstract = null == composite.getShortFormPart() || 0 == composite.getShortFormPart();
			XmlSchemaType xsdType = addCompositeType(schema, composite, isAbstract);
			if (!isAbstract) {
				XmlSchemaType xsdListType = addCorrespondingListType(schema, xsdType);
				addCorrespondingElement(schema, xsdType);
				addCorrespondingElement(schema, xsdListType);
			}
		} else if (dataType instanceof EnumerationType) {
			EnumerationType enumeration = (EnumerationType) dataType;
			XmlSchemaType xsdType = addEnumerationType(schema, enumeration);
			XmlSchemaType xsdListType = addCorrespondingListType(schema, xsdType);
			addCorrespondingElement(schema, xsdType);
			addCorrespondingElement(schema, xsdListType);
		} else if (dataType instanceof AttributeType) {
			AttributeType attribute = (AttributeType) dataType;
			XmlSchemaType xsdType = addAttributeType(schema, attribute);
			XmlSchemaType xsdListType = addCorrespondingListType(schema, xsdType);
			addCorrespondingElement(schema, xsdType);
			addCorrespondingElement(schema, xsdListType);
		} else if (dataType instanceof FundamentalType) {
			FundamentalType fundamental = (FundamentalType) dataType;
			addFundamentalType(schema, fundamental);
		}
	}

	/**
	 * Add a schema type corresponding to a MAL Composite to the schema.
	 *
	 * @param schema the schema to add the type to
	 * @param composite the MAL Composite for which the corresponding schema type shall be added
	 * @param isAbstract {@code true} if the composite is abstract, {@code false} otherwise
	 * @return the newly added schema type
	 */
	private static XmlSchemaType addCompositeType(XmlSchema schema, CompositeType composite, boolean isAbstract) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(composite.getName());
		xsdType.setAbstract(isAbstract);
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		QName xsdBaseTypeName = XSD_MAL_COMPOSITE;
		if (null != composite.getExtends()) {
			xsdBaseTypeName = toQName(composite.getExtends().getType());
		}
		xsdExtension.setBaseTypeName(xsdBaseTypeName);

		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdExtension.setParticle(xsdSequence);
		List<XmlSchemaSequenceMember> xsdSequenceItems = xsdSequence.getItems();
		for (NamedElementReferenceWithCommentType field : composite.getField()) {
			XmlSchemaElement xsdSeqElem = new XmlSchemaElement(schema, false);
			xsdSequenceItems.add(xsdSeqElem);
			xsdSeqElem.setName(field.getName());
			xsdSeqElem.setNillable(field.isCanBeNull());
			xsdSeqElem.setSchemaTypeName(toQName(field.getType()));
		}
		return xsdType;
	}

	/**
	 * Add a schema type corresponding to a MAL Enumeration to the schema.
	 *
	 * @param schema the schema to add the type to
	 * @param enumeration the MAL Enumeration for which the corresponding schema type shall be added
	 * @return the newly added schema type
	 */
	private static XmlSchemaType addEnumerationType(XmlSchema schema, EnumerationType enumeration) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(enumeration.getName());
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		xsdExtension.setBaseTypeName(XSD_MAL_ELEMENT);
		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdExtension.setParticle(xsdSequence);

		XmlSchemaSimpleType xsdEnumType = new XmlSchemaSimpleType(schema, true);
		xsdEnumType.setName(enumeration.getName() + "Enum");
		XmlSchemaSimpleTypeRestriction xsdRestriction = new XmlSchemaSimpleTypeRestriction();
		xsdRestriction.setBaseTypeName(Constants.XSD_STRING);
		List<XmlSchemaFacet> xsdRestrictionFacets = xsdRestriction.getFacets();
		for (EnumerationType.Item item : enumeration.getItem()) {
			XmlSchemaEnumerationFacet xsdEnumFacet = new XmlSchemaEnumerationFacet(item.getValue(), false);
			xsdRestrictionFacets.add(xsdEnumFacet);
		}
		xsdEnumType.setContent(xsdRestriction);

		XmlSchemaElement xsdSeqElem = new XmlSchemaElement(schema, false);
		xsdSequence.getItems().add(xsdSeqElem);
		xsdSeqElem.setName(enumeration.getName());
		xsdSeqElem.setSchemaTypeName(xsdEnumType.getQName());
		return xsdType;
	}

	/**
	 * Add a schema type corresponding to a MAL Attribute to the schema.
	 *
	 * @param schema the schema to add the type to
	 * @param attribute the MAL Attribute for which the corresponding schema type shall be added
	 * @return the newly added schema type
	 */
	private static XmlSchemaType addAttributeType(XmlSchema schema, AttributeType attribute) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(attribute.getName());
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		xsdExtension.setBaseTypeName(XSD_MAL_ATTRIBUTE);
		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdExtension.setParticle(xsdSequence);

		// map attribute to XSD type - unknown attributes are mapped to anyType
		QName xsdMappingTypeName = ATTRIBUTE_XSD_MAPPING.getOrDefault(attribute.getName(), Constants.XSD_ANYTYPE);
		XmlSchemaElement xsdSeqElem = new XmlSchemaElement(schema, false);
		xsdSequence.getItems().add(xsdSeqElem);
		xsdSeqElem.setName(attribute.getName());
		xsdSeqElem.setSchemaTypeName(xsdMappingTypeName);
		return xsdType;
	}

	/**
	 * Add a schema type corresponding to a MAL Fundamental to the schema.
	 *
	 * @param schema the schema to add the type to
	 * @param enumeration the MAL Fundamental for which the corresponding schema type shall be added
	 * @return the newly added schema type
	 */
	private static XmlSchemaType addFundamentalType(XmlSchema schema, FundamentalType fundamental) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(fundamental.getName());
		xsdType.setAbstract(true);

		if (null == fundamental.getExtends()) {
			return xsdType;
		}

		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		xsdExtension.setBaseTypeName(toQName(fundamental.getExtends().getType()));

		// PENDING: Not sure whether the attribute should go in the base type or in each Composite (5.6.8).
		if ("Composite".equals(fundamental.getName())) {
			XmlSchemaAttribute xsdShortFormAttr = new XmlSchemaAttribute(schema, false);
			xsdExtension.getAttributes().add(xsdShortFormAttr);
			xsdShortFormAttr.setName("type");
			xsdShortFormAttr.setSchemaTypeName(Constants.XSD_LONG);
		}
		return xsdType;
	}

	/**
	 * Add a schema type corresponding to a list of an existing schema type.
	 *
	 * @param schema the schema to add the type to
	 * @param xsdType the existing schema type for which another one shall be added to represent
	 * lists of that type
	 * @return the newly added schema list type
	 */
	private static XmlSchemaType addCorrespondingListType(XmlSchema schema, XmlSchemaType xsdType) {
		XmlSchemaComplexType xsdListType = new XmlSchemaComplexType(schema, true);
		xsdListType.setName(xsdType.getName() + "List");
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdListType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		xsdExtension.setBaseTypeName(XSD_MAL_COMPOSITE);
		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdExtension.setParticle(xsdSequence);

		XmlSchemaElement xsdSeqElem = new XmlSchemaElement(schema, false);
		xsdSequence.getItems().add(xsdSeqElem);
		xsdSeqElem.setName(xsdType.getName());
		xsdSeqElem.setSchemaTypeName(xsdType.getQName());
		xsdSeqElem.setMinOccurs(0);
		xsdSeqElem.setMaxOccurs(Long.MAX_VALUE);
		xsdSeqElem.setNillable(true);
		return xsdListType;
	}

	/**
	 * Add a schema element for an existing schema type which will carry the same name as the type.
	 *
	 * @param schema the schema to add the type to
	 * @param xsdType the existing schema type for which an schema element with the same name is
	 * added
	 * @return the newly added schema element
	 */
	private static XmlSchemaElement addCorrespondingElement(XmlSchema schema, XmlSchemaType xsdType) {
		XmlSchemaElement element = new XmlSchemaElement(schema, true);
		element.setName(xsdType.getName());
		element.setSchemaTypeName(xsdType.getQName());
		return element;
	}

	/**
	 * Add a new schema type for specifying MAL message bodies.
	 * <p>
	 * The MAL specification does not contain this data type, but the MAL-XML mapping specification
	 * does. Therefore, this type cannot be generated from the specification but has to be added
	 * manually here.
	 *
	 * @param schema the schema to add the type to
	 * @return the newly added body type
	 */
	private static XmlSchemaType addExtraMalBodyType(XmlSchema schema) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName("Body");
		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdType.setParticle(xsdSequence);
		XmlSchemaAny xsdSeqElem = new XmlSchemaAny();
		xsdSequence.getItems().add(xsdSeqElem);
		xsdSeqElem.setProcessContent(XmlSchemaContentProcessing.LAX);
		xsdSeqElem.setMinOccurs(0);
		xsdSeqElem.setMaxOccurs(Long.MAX_VALUE);
		return xsdType;
	}

	/**
	 * Create an XML namespace from a MAL area and service name.
	 *
	 * @param areaName the MAL area name, must not be {@code null}
	 * @param serviceName the MAL service name, may be {@code null}
	 * @return the XML namespace for elements in the given area and/or service
	 */
	private static String toNamespace(String areaName, String serviceName) {
		String namespace = MALXML_NAMESPACE_BASE + areaName;
		if (null != serviceName) {
			namespace += "/" + serviceName;
		}
		return namespace;
	}

	/**
	 * Create an XML qualified name from a MAL type reference.
	 *
	 * @param typeRef the MAL type reference
	 * @return the XML qualified name corresponding to the MAL type reference
	 */
	private static QName toQName(TypeReference typeRef) {
		String namespace = toNamespace(typeRef.getArea(), typeRef.getService());
		String typeName = typeRef.getName();
		if (typeRef.isList()) {
			typeName += "List";
		}
		return new QName(namespace, typeName);
	}

}
