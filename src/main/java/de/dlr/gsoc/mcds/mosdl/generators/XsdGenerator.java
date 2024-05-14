// Copyright 2020 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.generators;

import de.dlr.gsoc.mcds.mosdl.InteractionStage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentProcessing;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.ccsds.schema.serviceschema.AreaType;
import org.ccsds.schema.serviceschema.AttributeType;
import org.ccsds.schema.serviceschema.CapabilitySetType;
import org.ccsds.schema.serviceschema.CompositeType;
import org.ccsds.schema.serviceschema.EnumerationType;
import org.ccsds.schema.serviceschema.FundamentalType;
import org.ccsds.schema.serviceschema.NamedElementReferenceWithCommentType;
import org.ccsds.schema.serviceschema.ObjectFactory;
import org.ccsds.schema.serviceschema.OperationType;
import org.ccsds.schema.serviceschema.ServiceType;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Text;

/**
 * Class for generating MO XSD files describing data structures of an MO specification.
 * <p>
 * Each separate namespace is put in its own file, i.e. one file for each area and/or service that
 * define data structures.
 */
public class XsdGenerator extends Generator {

	private static final Logger logger = LoggerFactory.getLogger(XsdGenerator.class);

	private static final String META_KEY_FILENAME = "filename";
	private static final String META_KEY_IMPORTS = "imports";
	private static final String XSD_FILE_ENDING = ".xsd";
	private static final String MAL_AREA_NAME = "MAL";
	// Mismatch between 5.2.1 and 3.7.3.2.1! urn:ccsds:schema:mo:malxml vs. http://www.ccsds.org/schema/malxml/MAL (latter seems to be more common and is used by NASA implementation)
	private static final String MALXML_NAMESPACE_BASE = "http://www.ccsds.org/schema/malxml/";
	private static final String MALXML_NAMESPACE = toNamespace(MAL_AREA_NAME, null);
	private static final QName XSD_MAL_ELEMENT = new QName(MALXML_NAMESPACE, "Element");
	private static final QName XSD_MAL_ATTRIBUTE = new QName(MALXML_NAMESPACE, "Attribute");
	private static final QName XSD_MAL_COMPOSITE = new QName(MALXML_NAMESPACE, "Composite");
	private static final QName XSD_MAL_BODY = new QName(MALXML_NAMESPACE, "Body");
	private static final Map<String, QName> ATTRIBUTE_XSD_MAPPING = new HashMap<>();
	private static final NamespaceMap NAMESPACE_MAP = new NamespaceMap();
	private static final Map<String, String> WRITE_OPTIONS = new HashMap<>();
	private final boolean isIncludeDoc;
	private final boolean isCreateBodyTypes;
	private final Document documentationDocument;

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

		System.setProperty("org.apache.ws.commons.schema.protectReadOnlyCollections", "true");
		WRITE_OPTIONS.put("omit-xml-declaration", "no");
		WRITE_OPTIONS.put("indent", "yes");
	}

	/**
	 * Creates a new XML Schema generator.
	 *
	 * @param isIncludeDoc {@code true} to create annotations for documenting the schema,
	 * {@code false} otherwise
	 * @param isCreateBodyTypes {@code true} to create non-standard specialized message body types
	 * for operations, {@code false} otherwise
	 */
	public XsdGenerator(boolean isIncludeDoc, boolean isCreateBodyTypes) {
		this.isIncludeDoc = isIncludeDoc;
		this.isCreateBodyTypes = isCreateBodyTypes;
		if (isIncludeDoc) {
			try {
				documentationDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			} catch (ParserConfigurationException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			documentationDocument = null;
		}
	}

	@Override
	public void generate(SpecificationType spec, File targetDirectory) throws GeneratorException {
		logger.debug("Generating XSD file(s) into directory '{}'.", targetDirectory);

		int nFiles = 0;
		try {
			XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
			for (AreaType area : spec.getArea()) {
				if (null != area.getDataTypes()) {
					logger.debug("Generating schema for area '{}'.", area.getName());
					XmlSchema schema = getOrCreateSchema(schemaCollection, area, null);
					if (MAL_AREA_NAME.equals(area.getName())) {
						// XSD type and element for message body as defined in CCSDS 524.3-B-1, 3.7.3.2 need to be added explicitly.
						XmlSchemaType xsdType = addExtraMalBodyType(schema);
						addCorrespondingElement(schema, xsdType, false);
					}
					for (Object dataType : area.getDataTypes().getFundamentalOrAttributeOrComposite()) {
						addDataType(schema, dataType);
					}
				}
				for (ServiceType service : area.getService()) {
					XmlSchema schema = null;
					if (null != service.getDataTypes()) {
						logger.debug("Generating schema for service '{}' of area '{}'.", service.getName(), area.getName());
						schema = getOrCreateSchema(schemaCollection, area, service);
						for (Object dataType : service.getDataTypes().getCompositeOrEnumeration()) {
							addDataType(schema, dataType);
						}
					}
					if (isCreateBodyTypes) {
						for (CapabilitySetType cs : service.getCapabilitySet()) {
							for (OperationType op : cs.getSendIPOrSubmitIPOrRequestIP()) {
								// TODO: Implement handling of error message bodies.
								for (MessageDetails msgDetails : MessageDetails.fromOp(op)) {
									if (null == schema) {
										schema = getOrCreateSchema(schemaCollection, area, service);
									}
									XmlSchemaType xsdType = addMessageBody(schema, op, msgDetails);
//									addCorrespondingElement(schema, xsdType, false); // TODO: Remove element, only needed for easier testing
								}
							}
						}
					}
				}
			}

			for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
				Map<Object, Object> metaInfo = schema.getMetaInfoMap();
				if (null == metaInfo) {
					continue;
				}
				String filename = (String) metaInfo.get(META_KEY_FILENAME);
				if (null == filename) {
					continue;
				}
				File targetFile = new File(targetDirectory, filename + XSD_FILE_ENDING);

				@SuppressWarnings("unchecked")
				Set<String> schemaImports = (Set<String>) metaInfo.get(META_KEY_IMPORTS);
				if (null != schemaImports) {
					schemaImports.add(MALXML_NAMESPACE); // always add import for MAL types
					for (String namespace : schemaImports) {
						// remove import if it would import the target namespace of the current schema
						if (Objects.equals(namespace, schema.getTargetNamespace())) {
							continue;
						}
						XmlSchemaImport xsdImport = new XmlSchemaImport(schema);
						xsdImport.setNamespace(namespace);
					}
				}
				logger.debug("Writing schema file '{}'.", targetFile);
				try (OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile))) {
					schema.write(os, WRITE_OPTIONS);
				}
				nFiles++;
			}
		} catch (IOException ex) {
			throw new GeneratorException(ex);
		}
		logger.debug("Generated {} XSD file(s) for the supplied specification(s) into directory '{}'.", nFiles, targetDirectory);
	}

	/**
	 * Get an XML schema from a schema collection or create a new one if it is not present yet for
	 * the given area and/or service.
	 *
	 * @param schemaCollection the schema collection to get the schema from/add the schema to
	 * @param area the MAL area containing the data structures to add, must not be {@code null}
	 * @param service the MAL service containing the data structures to add, may be {@code null}
	 * @return an XML schema suitable for adding the transformed data structures of given area
	 * and/or service
	 */
	private static XmlSchema getOrCreateSchema(XmlSchemaCollection schemaCollection, AreaType area, ServiceType service) {
		String serviceName = null == service ? null : service.getName();
		String namespace = toNamespace(area.getName(), serviceName);
		XmlSchema schema = schemaCollection.schemaForNamespace(namespace);
		if (null == schema) {
			serviceName = null == serviceName ? "" : serviceName;
			String filename = area.getName() + serviceName;
			schema = new XmlSchema(namespace, schemaCollection);
			schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
			schema.setAttributeFormDefault(XmlSchemaForm.QUALIFIED);
			schema.setInputEncoding(StandardCharsets.UTF_8.name());
			schema.setNamespaceContext(NAMESPACE_MAP);
			schema.addMetaInfo(META_KEY_FILENAME, filename);
			schema.addMetaInfo(META_KEY_IMPORTS, new LinkedHashSet<String>());
		}
		return schema;
	}

	/**
	 * Process a MAL data structure and add all necessary elements to the schema.
	 * <p>
	 * Necessary elements are:
	 * <ul>
	 * <li>a new schema type for the data structure,
	 * <li>a new schema type for the list of the data structure,
	 * <li>a new element for using the data structure as MAL message body element,
	 * <li>a new element for using a list of the data structure as MAL body element.
	 * </ul>
	 *
	 * @param schema the schema to add all required elements to
	 * @param dataType the MAL data structure to process
	 */
	private void addDataType(XmlSchema schema, Object dataType) {
		XmlSchemaComplexType xsdType;
		if (dataType instanceof CompositeType) {
			CompositeType composite = (CompositeType) dataType;
			boolean isAbstract = null == composite.getShortFormPart() || 0 == composite.getShortFormPart();
			xsdType = addCompositeType(schema, composite, isAbstract);
		} else if (dataType instanceof EnumerationType) {
			EnumerationType enumeration = (EnumerationType) dataType;
			xsdType = addEnumerationType(schema, enumeration);
		} else if (dataType instanceof AttributeType) {
			AttributeType attribute = (AttributeType) dataType;
			xsdType = addAttributeType(schema, attribute);
		} else if (dataType instanceof FundamentalType) {
			FundamentalType fundamental = (FundamentalType) dataType;
			xsdType = addFundamentalType(schema, fundamental);
		} else {
			logger.warn("Unsupported data type '{}' cannot be mapped to XML Schema type.", null == dataType ? "null" : dataType.getClass());
			return;
		}
		XmlSchemaComplexType xsdListType = addCorrespondingListType(schema, xsdType);
		// add elements for use in message bodies
		addCorrespondingElement(schema, xsdType, true);
		addCorrespondingElement(schema, xsdListType, true);
	}

	/**
	 * Add a schema type corresponding to a MAL Composite to the schema.
	 *
	 * @param schema the schema to add the type to
	 * @param composite the MAL Composite for which the corresponding schema type shall be added
	 * @param isAbstract {@code true} if the composite is abstract, {@code false} otherwise
	 * @return the newly added schema type
	 */
	private XmlSchemaComplexType addCompositeType(XmlSchema schema, CompositeType composite, boolean isAbstract) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(composite.getName());
		xsdType.setAbstract(isAbstract);
		addDoc(xsdType, composite.getComment());
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		QName xsdBaseTypeName = XSD_MAL_COMPOSITE;
		if (null != composite.getExtends()) {
			xsdBaseTypeName = toQName(schema, composite.getExtends().getType());
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
			xsdSeqElem.setSchemaTypeName(toQName(schema, field.getType()));
			addDoc(xsdSeqElem, field.getComment());
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
	private XmlSchemaComplexType addEnumerationType(XmlSchema schema, EnumerationType enumeration) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(enumeration.getName());
		addDoc(xsdType, enumeration.getComment());
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
			addDoc(xsdEnumFacet, item.getComment());
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
	private XmlSchemaComplexType addAttributeType(XmlSchema schema, AttributeType attribute) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(attribute.getName());
		addDoc(xsdType, attribute.getComment());
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
	private XmlSchemaComplexType addFundamentalType(XmlSchema schema, FundamentalType fundamental) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		xsdType.setName(fundamental.getName());
		xsdType.setAbstract(true);
		addDoc(xsdType, fundamental.getComment());

		if (null == fundamental.getExtends()) {
			return xsdType;
		}

		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentExtension xsdExtension = new XmlSchemaComplexContentExtension();
		xsdContent.setContent(xsdExtension);
		xsdExtension.setBaseTypeName(toQName(schema, fundamental.getExtends().getType()));

		// PENDING: Not sure whether the attribute should go in the base type or in each Composite (5.6.8).
		if ("Composite".equals(fundamental.getName())) {
			XmlSchemaAttribute xsdShortFormAttr = new XmlSchemaAttribute(schema, false);
			xsdExtension.getAttributes().add(xsdShortFormAttr);
			xsdShortFormAttr.setName("type");
			xsdShortFormAttr.setSchemaTypeName(Constants.XSD_LONG);
			// xsdShortFormAttr.setUse(XmlSchemaUse.REQUIRED);
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
	private static XmlSchemaComplexType addCorrespondingListType(XmlSchema schema, XmlSchemaComplexType xsdType) {
		XmlSchemaComplexType xsdListType = new XmlSchemaComplexType(schema, true);
		xsdListType.setName(xsdType.getName() + "List");
		xsdListType.setAbstract(xsdType.isAbstract());
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
	 * @param isNillable {@code true} if the element shall be nillable, {@code false} otherwise
	 * @return the newly added schema element
	 */
	private static XmlSchemaElement addCorrespondingElement(XmlSchema schema, XmlSchemaType xsdType, boolean isNillable) {
		XmlSchemaElement element = new XmlSchemaElement(schema, true);
		element.setName(xsdType.getName());
		element.setSchemaTypeName(xsdType.getQName());
		element.setNillable(isNillable);
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
		xsdType.setName(XSD_MAL_BODY.getLocalPart());
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
	 * Add a schema type corresponding to the message body of one of the messages belonging to a
	 * certain operation.
	 * <p>
	 * This type is not defined in CCSDS 524.3-B-1, but is compatible to the standard Body type
	 * defined in that document because it is derived from it by restriction. The type is put in the
	 * same namespace as service-level data types and is named
	 * {@code {opName}_{INTERACTION_STAGE}_Body}, where {@code {opName}} is the operation name and
	 * {@code {INTERACTION_STAGE}} is the interaction stage name in upper case.
	 *
	 * @param schema the schema to add the type to
	 * @param op the operation to which the message in {@code msgDetails} belongs to
	 * @param msgDetails the details of one of the messages belonging to a certain operation
	 * @return the newly added schema type
	 */
	private XmlSchemaType addMessageBody(XmlSchema schema, OperationType op, MessageDetails msgDetails) {
		XmlSchemaComplexType xsdType = new XmlSchemaComplexType(schema, true);
		String xsdTypeName = String.format("%s_%s_%s", op.getName(), msgDetails.getStage().name(), XSD_MAL_BODY.getLocalPart());
		xsdType.setName(xsdTypeName);
		addDoc(xsdType, msgDetails.getComment());
		XmlSchemaComplexContent xsdContent = new XmlSchemaComplexContent();
		xsdType.setContentModel(xsdContent);
		XmlSchemaComplexContentRestriction xsdRestriction = new XmlSchemaComplexContentRestriction();
		xsdContent.setContent(xsdRestriction);
		xsdRestriction.setBaseTypeName(XSD_MAL_BODY);

		XmlSchemaSequence xsdSequence = new XmlSchemaSequence();
		xsdRestriction.setParticle(xsdSequence);
		List<XmlSchemaSequenceMember> xsdSequenceItems = xsdSequence.getItems();

		List<NamedElementReferenceWithCommentType> fields;
		if (msgDetails.getStage() == InteractionStage.PUBSUB_PUBLISH || msgDetails.getStage() == InteractionStage.PUBSUB_NOTIFY) {
			// According to MAL Tables 3-27 and 3-29: Add extra message fields for PUBLISH and NOTIFY and transform given message fields to lists.
			fields = new ArrayList<>();
			ObjectFactory of = new ObjectFactory();
			if (msgDetails.getStage() == InteractionStage.PUBSUB_NOTIFY) {
				TypeReference subscriptionIdType = of.createTypeReference();
				subscriptionIdType.setArea(MAL_AREA_NAME);
				subscriptionIdType.setName("Identifier");
				NamedElementReferenceWithCommentType subscriptionIdField = of.createNamedElementReferenceWithCommentType();
				subscriptionIdField.setName("subscriptionId");
				subscriptionIdField.setType(subscriptionIdType);
				fields.add(subscriptionIdField);
			}

			TypeReference updateHeaderListType = of.createTypeReference();
			updateHeaderListType.setArea(MAL_AREA_NAME);
			updateHeaderListType.setName("UpdateHeader");
			updateHeaderListType.setList(Boolean.TRUE);
			NamedElementReferenceWithCommentType updateHeadersField = of.createNamedElementReferenceWithCommentType();
			updateHeadersField.setName("updateHeaders");
			updateHeadersField.setType(updateHeaderListType);
			fields.add(updateHeadersField);
			for (NamedElementReferenceWithCommentType field : msgDetails.getFields()) {
				// deeply copy field and always set list to true
				TypeReference typeRefCopy = of.createTypeReference();
				TypeReference typeRef = field.getType();
				typeRefCopy.setArea(typeRef.getArea());
				typeRefCopy.setService(typeRef.getService());
				typeRefCopy.setName(typeRef.getName());
				typeRefCopy.setList(Boolean.TRUE);
				NamedElementReferenceWithCommentType fieldCopy = of.createNamedElementReferenceWithCommentType();
				fieldCopy.setName(field.getName());
				fieldCopy.setComment(field.getComment());
				fieldCopy.setCanBeNull(field.isCanBeNull());
				fieldCopy.setType(typeRefCopy);
				fields.add(fieldCopy);
			}
		} else {
			fields = msgDetails.getFields();
		}

		for (NamedElementReferenceWithCommentType field : fields) {
			XmlSchemaElement xsdSeqElem = new XmlSchemaElement(schema, false);
			xsdSeqElem.getRef().setTargetQName(toQName(schema, field.getType()));
			xsdSequenceItems.add(xsdSeqElem);
			addDoc(xsdSeqElem, field.getComment());
		}
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
	 * Create an XML qualified name from a MAL type reference and adds namespace to the set of
	 * namespaces to be imported.
	 *
	 * @param schema the schema that holds the set of namespaces to be imported
	 * @param typeRef the MAL type reference
	 * @return the XML qualified name corresponding to the MAL type reference
	 */
	private static QName toQName(XmlSchema schema, TypeReference typeRef) {
		String namespace = toNamespace(typeRef.getArea(), typeRef.getService());
		String typeName = typeRef.getName();
		if (typeRef.isList()) {
			typeName += "List";
		}

		Map<Object, Object> metaInfo = schema.getMetaInfoMap();
		if (null != metaInfo) {
			@SuppressWarnings("unchecked")
			Set<String> schemaImports = (Set<String>) metaInfo.get(META_KEY_IMPORTS);
			if (null != schemaImports) {
				schemaImports.add(namespace);
			}
		}

		return new QName(namespace, typeName);
	}

	/**
	 * Add a simple documentation annotation element to a given schema element.
	 *
	 * @param annotatableElement the schema element to add the documentation element to
	 * @param docText the text to be used as documentation. May be {@code null}, in which case no
	 * annotation is added
	 */
	private void addDoc(XmlSchemaAnnotated annotatableElement, String docText) {
		if (!isIncludeDoc || null == docText) {
			return;
		}
		XmlSchemaAnnotation xsdAnnotation = new XmlSchemaAnnotation();
		annotatableElement.setAnnotation(xsdAnnotation);
		XmlSchemaDocumentation xsdDoc = new XmlSchemaDocumentation();
		xsdAnnotation.getItems().add(xsdDoc);

		Text textNode = documentationDocument.createTextNode(docText);
		DocumentFragment docFragment = documentationDocument.createDocumentFragment();
		docFragment.appendChild(textNode);
		xsdDoc.setMarkup(docFragment.getChildNodes());

	}

}
