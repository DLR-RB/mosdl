// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl.generators;

import de.dlr.gsoc.mcds.mosdl.InteractionStage;
import de.dlr.gsoc.mcds.mosdl.InteractionType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.ccsds.schema.serviceschema.AnyTypeReference;
import org.ccsds.schema.serviceschema.InvokeOperationType;
import org.ccsds.schema.serviceschema.NamedElementReferenceWithCommentType;
import org.ccsds.schema.serviceschema.OperationType;
import org.ccsds.schema.serviceschema.ProgressOperationType;
import org.ccsds.schema.serviceschema.PubSubOperationType;
import org.ccsds.schema.serviceschema.RequestOperationType;
import org.ccsds.schema.serviceschema.SendOperationType;
import org.ccsds.schema.serviceschema.SpecificationType;
import org.ccsds.schema.serviceschema.SubmitOperationType;

/**
 * Base class for any generator that generates artifacts from an MO service specification.
 * <p>
 * Usually a generator should be configured when creating it.
 */
public abstract class Generator {

	/**
	 * Convenience method to get the interaction pattern of an operation.
	 *
	 * @param op the operation to get the interaction pattern for
	 * @return the interaction pattern of the supplied operation
	 */
	public static InteractionType getInteractionType(OperationType op) {
		if (op instanceof SendOperationType) {
			return InteractionType.SEND;
		} else if (op instanceof SubmitOperationType) {
			return InteractionType.SUBMIT;
		} else if (op instanceof RequestOperationType) {
			return InteractionType.REQUEST;
		} else if (op instanceof InvokeOperationType) {
			return InteractionType.INVOKE;
		} else if (op instanceof ProgressOperationType) {
			return InteractionType.PROGRESS;
		} else if (op instanceof PubSubOperationType) {
			return InteractionType.PUBSUB;
		}
		throw new IllegalArgumentException("Operation is of unknown interaction type.");
	}

	/**
	 * Performs generation of artifacts based on a supplied MO service specification.
	 *
	 * @param spec the specification to create artifacts for
	 * @param targetDirectory the directory where to put the generated artifacts. Depending on the
	 * generator it can also be possible to supply a file here.
	 * @throws GeneratorException thrown if any error occurs during generating the artifacts
	 */
	public abstract void generate(SpecificationType spec, File targetDirectory) throws GeneratorException;

	/**
	 * Convenience class for holding details about a single message of an operation.
	 * <p>
	 * Use {@link #fromOp(OperationType)} to generate a list of message detail instances for an
	 * operation. Each message detail instance corresponds to one of the operation's messages.
	 */
	public static class MessageDetails {

		private final InteractionStage stage;
		private final String comment;
		private final List<NamedElementReferenceWithCommentType> fields;

		private MessageDetails(InteractionStage stage, String comment, AnyTypeReference atr) {
			this.stage = stage;
			this.comment = comment;
			this.fields = anyTypeRefsToFields(atr);
		}

		/**
		 * Gets the interaction stage of this message.
		 *
		 * @return the interaction stage of this message
		 */
		public InteractionStage getStage() {
			return stage;
		}

		/**
		 * Gets the message documentation.
		 *
		 * @return the message documentation
		 */
		public String getComment() {
			return comment;
		}

		/**
		 * Gets the message body parts.
		 *
		 * @return the list of message body parts
		 */
		public List<NamedElementReferenceWithCommentType> getFields() {
			return fields;
		}

		/**
		 * Creates a list of message detail instances for an operation.
		 * <p>
		 * Each message detail instance corresponds to one of the operation's messages.
		 *
		 * @param op the operation to get the message details for
		 * @return a list of message details corresponding to the operation's messages
		 */
		public static List<MessageDetails> fromOp(OperationType op) {
			List<MessageDetails> messages = new ArrayList<>();
			if (op instanceof SendOperationType) {
				AnyTypeReference atrSend = ((SendOperationType) op).getMessages().getSend();
				messages.add(new MessageDetails(InteractionStage.SEND, atrSend.getComment(), atrSend));
			} else if (op instanceof SubmitOperationType) {
				AnyTypeReference atrSubmit = ((SubmitOperationType) op).getMessages().getSubmit();
				messages.add(new MessageDetails(InteractionStage.SUBMIT, atrSubmit.getComment(), atrSubmit));
			} else if (op instanceof RequestOperationType) {
				RequestOperationType.Messages msgs = ((RequestOperationType) op).getMessages();
				AnyTypeReference atrRequest = msgs.getRequest();
				AnyTypeReference atrResponse = msgs.getResponse();
				messages.add(new MessageDetails(InteractionStage.REQUEST, atrRequest.getComment(), atrRequest));
				messages.add(new MessageDetails(InteractionStage.REQUEST_RESPONSE, atrResponse.getComment(), atrResponse));
			} else if (op instanceof InvokeOperationType) {
				InvokeOperationType.Messages msgs = ((InvokeOperationType) op).getMessages();
				AnyTypeReference atrInvoke = msgs.getInvoke();
				AnyTypeReference atrAck = msgs.getAcknowledgement();
				AnyTypeReference atrResponse = msgs.getResponse();
				messages.add(new MessageDetails(InteractionStage.INVOKE, atrInvoke.getComment(), atrInvoke));
				messages.add(new MessageDetails(InteractionStage.INVOKE_ACK, atrAck.getComment(), atrAck));
				messages.add(new MessageDetails(InteractionStage.INVOKE_RESPONSE, atrResponse.getComment(), atrResponse));
			} else if (op instanceof ProgressOperationType) {
				ProgressOperationType.Messages msgs = ((ProgressOperationType) op).getMessages();
				AnyTypeReference atrProgress = msgs.getProgress();
				AnyTypeReference atrAck = msgs.getAcknowledgement();
				AnyTypeReference atrUpdate = msgs.getUpdate();
				AnyTypeReference atrResponse = msgs.getResponse();
				messages.add(new MessageDetails(InteractionStage.PROGRESS, atrProgress.getComment(), atrProgress));
				messages.add(new MessageDetails(InteractionStage.PROGRESS_ACK, atrAck.getComment(), atrAck));
				messages.add(new MessageDetails(InteractionStage.PROGRESS_UPDATE, atrUpdate.getComment(), atrUpdate));
				messages.add(new MessageDetails(InteractionStage.PROGRESS_RESPONSE, atrResponse.getComment(), atrResponse));
			} else if (op instanceof PubSubOperationType) {
				AnyTypeReference atrPublish = ((PubSubOperationType) op).getMessages().getPublishNotify();
				AnyTypeReference atrNotify = atrPublish;
				messages.add(new MessageDetails(InteractionStage.PUBSUB_PUBLISH, atrPublish.getComment(), atrPublish));
				messages.add(new MessageDetails(InteractionStage.PUBSUB_NOTIFY, atrNotify.getComment(), atrNotify));
			}
			return messages;
		}

		private static List<NamedElementReferenceWithCommentType> anyTypeRefsToFields(AnyTypeReference atr) {
			List<NamedElementReferenceWithCommentType> fields = new ArrayList<>();
			for (Object o : atr.getAny()) {
				JAXBElement jObject = (JAXBElement) o;
				if (NamedElementReferenceWithCommentType.class.isAssignableFrom(jObject.getDeclaredType())) {
					NamedElementReferenceWithCommentType field = (NamedElementReferenceWithCommentType) jObject.getValue();
					fields.add(field);
				}
			}
			return fields;
		}
	}

}
