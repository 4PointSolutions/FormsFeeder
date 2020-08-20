package com._4point.aem.formsfeeder.core.datasource.serialization;

import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.DSL_ELEMENT_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.ATTR_ELEMENT_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.ATTR_NAME_ATTR_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.ATTR_VALUE_ATTR_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.CONTENT_ELEMENT_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.CONTENT_TYPE_ATTR_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.DECODER;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.DS_ELEMENT_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.FILENAME_ATTR_NAME;
import static com._4point.aem.formsfeeder.core.datasource.serialization.XmlDataSourceListConstants.XmlDataSourceConstants.NAME_ATTR_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;

public class XmlDataSourceListDecoder extends XmlDecoder {
	public static final MimeType DSL_MIME_TYPE = XmlDataSourceListConstants.DSL_MIME_TYPE;
	public static final String DSL_MIME_TYPE_STR = XmlDataSourceListConstants.DSL_MIME_TYPE_STR;

	private XmlDataSourceListDecoder(XMLStreamReader xsr) {
		super(xsr);
	}

	/**
	 * Decoder Context object.  This is the context required by the Decoder State machine.  It uses the context to keep track
	 * of data that is shared between different states.
	 *
	 */
	private static class DecoderContext {
		private final XMLStreamReader xsr;
		private DataSourceList.Builder dslBuilder = null;
		private DataSourceDecoderContext dsDc = null;
		private ForeignElementDecoderContext feDc = null;
		
		private DecoderContext(XMLStreamReader xsr) {
			super();
			this.xsr = xsr;
		}

		private DecoderContext initializeDslBuilder() {
			this.dslBuilder = DataSourceList.builder();
			return this;
		}
		
		private static DecoderContext from(XMLStreamReader xsr) {
			return new DecoderContext(xsr);
		}
		
		private static class DataSourceDecoderContext {
			String name = null;
			MimeType contentType = null;
			Path filename = null;
			Map<String, String> attributes = new HashMap<>();
			byte[] content = new byte[0];
			
			public void moveDataSourceContextIntoDataSourceList(DataSourceList.Builder dslBuilder) {
				if (name != null) {
					if (contentType != null && !attributes.isEmpty() && filename != null) {
						dslBuilder.add(name, content, contentType, filename, attributes);
					} else if (contentType != null && attributes.isEmpty() && filename != null) {
						dslBuilder.add(name, content, contentType, filename);		// No attributes
					} else if (contentType != null && !attributes.isEmpty() && filename == null) {
						dslBuilder.add(name, content, contentType, attributes);		// No filename
					} else if (contentType == null && !attributes.isEmpty() && filename != null) {
						dslBuilder.add(name, content, filename, attributes);		// No contentType
					} else if (contentType != null && attributes.isEmpty() && filename == null) {
						dslBuilder.add(name, content, contentType);					// No attributes and no filename
					} else if (contentType == null && !attributes.isEmpty() && filename == null) {
						dslBuilder.add(name, content, attributes);					// No contentType and no filename
					} else if (contentType == null && attributes.isEmpty() && filename != null) {
						dslBuilder.add(name, content, filename);					// No contentType and no attributes
					}  else {
						dslBuilder.add(name, content);								// No contentType, no filename and no attributes
					}
				}
			}
		}
		
		private DecoderContext initializeDataSourceContext() {
			this.dsDc = new DataSourceDecoderContext();
			return this;
		}
		
		private static class ForeignElementDecoderContext {
			private final String foreignElementName;
			private final DecoderState previousState;
			private ForeignElementDecoderContext(String foreignElementName, DecoderState previousState) {
				super();
				this.foreignElementName = foreignElementName;
				this.previousState = previousState;
			}
		}
		
		private void pushForeignElementState(String foreignElementName, DecoderState previousState) {
			this.feDc = new ForeignElementDecoderContext(foreignElementName,previousState);
		}
		
		private DecoderState popForeignElementState() {
			DecoderState prevState = Objects.requireNonNull(this.feDc, "Tried to pop ForeignObjectState without a corresponding push!").previousState;
			this.feDc = null;
			return prevState;
		}
		
		private String foreignElementName() {
			return this.feDc.foreignElementName;
		}
	}
	
	/**
	 * Interface implemented by the DecoderState enum for each state.
	 *
	 */
	@FunctionalInterface
	private interface DecoderStateProcessor {
		public DecoderState process(int nextItem, DecoderContext decoderContext) throws XMLStreamException;
	}

	/**
	 * State machine that decodes an XML stream containing an XML-encoded DataSourceList.
	 * 
	 * It utilizes a DecoderContext object to contain context variables. 
	 *
	 */
	private enum DecoderState implements DecoderStateProcessor {
		InitialState(DecoderState::initialState), 
		EndState(DecoderState::endState),
		LookingForDataSourceElement(DecoderState::lookingForDataSourceElement),
		LookingInsideDataSourceElement(DecoderState::lookingInsideDataSourceElement),
		IgnoringForeignElement(DecoderState::ignoringForeignElement)
		;
		
		private final BiFunction<Integer, DecoderContext, DecoderState> function;

		private DecoderState(BiFunction<Integer, DecoderContext, DecoderState> function) {
			this.function = function;
		}

		private static DecoderState initialState(int nextItem, DecoderContext ctx) {
			if (nextItem == XMLStreamReader.START_ELEMENT && ctx.xsr.getLocalName().equals(DSL_ELEMENT_NAME)) {
				// Found DataSourceList so initialize it and start looking for DataSource elements.
				ctx.initializeDslBuilder();
				return DecoderState.LookingForDataSourceElement;
			}
			return DecoderState.InitialState;
		}

		private static DecoderState endState(int nextItem, DecoderContext ctx) {
			return DecoderState.EndState;
		}

		private static DecoderState lookingForDataSourceElement(int nextItem, DecoderContext ctx) {
			if (nextItem == XMLStreamReader.START_ELEMENT) {
				if (ctx.xsr.getLocalName().equals(DS_ELEMENT_NAME)) {
					decodeDataSourceElement(ctx);
					return DecoderState.LookingInsideDataSourceElement;
				} else {
					// Some other element START_ELEMENT
					ctx.pushForeignElementState(ctx.xsr.getLocalName(), DecoderState.LookingForDataSourceElement);
					return DecoderState.IgnoringForeignElement;
				}
			}
			if (nextItem == XMLStreamReader.END_ELEMENT && ctx.xsr.getLocalName().equals(DSL_ELEMENT_NAME)) {
				return DecoderState.EndState;
			}
			return DecoderState.LookingForDataSourceElement;
		}

		private static DecoderState lookingInsideDataSourceElement(int nextItem, DecoderContext ctx) {
			if ((nextItem == XMLStreamReader.END_ELEMENT && ctx.xsr.getLocalName() == DS_ELEMENT_NAME)) {
				ctx.dsDc.moveDataSourceContextIntoDataSourceList(ctx.dslBuilder);
				ctx.dsDc = null;	// Clear the DataSource DecoderContext.
				return DecoderState.LookingForDataSourceElement;
			}
			if (nextItem == XMLStreamReader.START_ELEMENT) {
				switch (ctx.xsr.getLocalName()) {
				case ATTR_ELEMENT_NAME:
					decodeAttribute(ctx, ctx.dsDc.attributes);
					break;
				case CONTENT_ELEMENT_NAME:
					ctx.dsDc.content = decodeContent(ctx);
					break;
				default:
					// Some other element START_ELEMENT
					ctx.pushForeignElementState(ctx.xsr.getLocalName(), DecoderState.LookingInsideDataSourceElement);
					return DecoderState.IgnoringForeignElement;
				}
			}
			return DecoderState.LookingInsideDataSourceElement;
		}
		
		private static DecoderState ignoringForeignElement(int nextItem, DecoderContext ctx) {
			if ((nextItem == XMLStreamReader.END_ELEMENT && ctx.xsr.getLocalName() == ctx.foreignElementName())) {
				return ctx.popForeignElementState();
			}
			return DecoderState.IgnoringForeignElement;
		}
		
		@Override
		public DecoderState process(int nextItem, DecoderContext decoderContext) throws XMLStreamException {
			return function.apply(nextItem, decoderContext);
		}

		private static void decodeDataSourceElement(DecoderContext ctx) {
			ctx.initializeDataSourceContext();
			// Found a DataSource, so process it.
			int numAttr = ctx.xsr.getAttributeCount();
			for (int i = 0; i < numAttr; i++) {
				String attrName = ctx.xsr.getAttributeLocalName(i);
				switch (attrName) {
				case NAME_ATTR_NAME:
					ctx.dsDc.name = ctx.xsr.getAttributeValue(i);
					break;
				case CONTENT_TYPE_ATTR_NAME:
					ctx.dsDc.contentType = MimeType.of(ctx.xsr.getAttributeValue(i));
					break;
				case FILENAME_ATTR_NAME:
					ctx.dsDc.filename = Paths.get(ctx.xsr.getAttributeValue(i));
					break;
				}
			}
		}

		private static void decodeAttribute(DecoderContext ctx, Map<String,String> attributes) {
			String name = null;
			String value = null;
			int numAttr = ctx.xsr.getAttributeCount();
			for (int i = 0; i < numAttr; i++) {
				String attrName = ctx.xsr.getAttributeLocalName(i);
				switch (attrName) {
				case ATTR_NAME_ATTR_NAME:
					name = ctx.xsr.getAttributeValue(i);
					break;
				case ATTR_VALUE_ATTR_NAME:
					value = ctx.xsr.getAttributeValue(i);
					break;
				}
			}
			if (name != null) {
				attributes.put(name, value != null ? value : "");	// Put the name and value (empty string if there was no value).
			}
		}
		
		private static byte[] decodeContent(DecoderContext ctx) {
			try {
				String elementText = ctx.xsr.getElementText();
				if (elementText != null && !elementText.trim().isEmpty()) {
					return DECODER.decode(elementText);
				}
				return new byte[0];
			} catch (XMLStreamException e) {
				String msg = e.getMessage();
				throw new IllegalStateException("XML Error while extracting DataSource content text. (" + (msg != null ? msg : "null") + ").", e);
			}
		}
	}
	
	/**
	 * Read and decode a DataSourceList
	 * 
	 * @return
	 * @throws XMLStreamException 
	 */
	public Optional<DataSourceList> decode() throws XMLStreamException {

		DecoderContext ctx = DecoderContext.from(xsr);
		DecoderState currentState = DecoderState.InitialState;
		while(xsr.hasNext()) {
			if (currentState == DecoderState.InitialState || currentState == DecoderState.EndState || currentState == DecoderState.IgnoringForeignElement) {
				currentState = currentState.process(xsr.next(), ctx);
			} else {
				currentState = currentState.process(xsr.nextTag(), ctx);
			}
		}

		return Optional.ofNullable(ctx.dslBuilder).map(DataSourceList.Builder::build);
	}

	/**
	 * Note: Calling close on the XmlSourceEncoder *does not* close the underlying output stream.  It mearly closes off the
	 * encoding of this data source.
	 *
	 */
	@Override
	public void close() throws IOException {
		try {
			xsr.close();
		} catch (XMLStreamException e) {
			String msg = e.getMessage();
			throw new IOException("Error while closing XMLStream (" + (msg == null ? "null" : msg) + ").", e);
		}
	}

	public static XmlDataSourceListDecoder wrap(XMLStreamReader xsr) {
		return new XmlDataSourceListDecoder(Objects.requireNonNull(xsr, "XMLStreamReader cannot be null."));
	}
	
	public static XmlDataSourceListDecoder wrap(InputStream is) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamReader(is));
	}

	public static XmlDataSourceListDecoder wrap(Reader reader) throws XMLStreamException, FactoryConfigurationError {
		return wrap(toXmlStreamReader(reader));
	}
}
