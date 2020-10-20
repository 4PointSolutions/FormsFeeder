package com._4point.aem.formsfeeder.server.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com._4point.aem.formsfeeder.server.support.FormsFeederData.FormsFeederDataException;

public class FormsFeederData {
	private static final String FORMSFEEDER_ELEMENT =  "FormsFeeder";
	private static final String PLUGIN_NAME_ELEMENT =  "Plugin";
	private static final String FORMSFEEDER_XPATH_STRING = "//" + FORMSFEEDER_ELEMENT;	// String containing XPath to FormsFeeder element 

	private static final XPath XPATH_PROCESSOR = XPathFactory.newInstance().newXPath();
	private static final XPathExpression FORMSFEEDER_XPATH;
	private static final XPathExpression PLUGIN_NAME_XPATH;
	static {
		try {
			FORMSFEEDER_XPATH = XPATH_PROCESSOR.compile(FORMSFEEDER_XPATH_STRING);
			PLUGIN_NAME_XPATH = XPATH_PROCESSOR.compile(PLUGIN_NAME_ELEMENT);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Unable to initialize XPaths.", e);
		}
	}

	private final Node formsFeederNode;

	private FormsFeederData(Node formsFeederNode) {
		super();
		this.formsFeederNode = formsFeederNode;
	}
	
	public Optional<String> pluginName() {
		try {
			String name = PLUGIN_NAME_XPATH.evaluate(formsFeederNode);
			name = name != null ? name.strip() : name;	// Strip blanks off the name
			return (name == null || name.isBlank()) ? Optional.empty() : Optional.of(name);
		} catch (XPathExpressionException e) {
			throw new FormsFeederDataException("Unable to locate plugin name.", e);
		}
	}

	private static FormsFeederData from(Node formsFeederNode) {
		return new FormsFeederData(formsFeederNode);
	}

	public static Optional<FormsFeederData> from(byte[] xmlData) {
		return from(new ByteArrayInputStream(xmlData));
	}
	
	public static Optional<FormsFeederData> from(InputStream is) {
		Document xmlDoc;
		try {
			xmlDoc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new FormsFeederDataException(e);
		}
		return from(xmlDoc);
	}
	
	public static Optional<FormsFeederData> from(Document xmlDoc) {
		Node dom;
		try {
			dom = (Node)FORMSFEEDER_XPATH.evaluate(xmlDoc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new FormsFeederDataException(e);
		}
		return dom == null ? Optional.empty() : Optional.of(from(dom)); 
	}

	@SuppressWarnings("serial")
	public static class FormsFeederDataException extends RuntimeException {

		public FormsFeederDataException() {
		}

		public FormsFeederDataException(String message) {
			super(message);
		}

		public FormsFeederDataException(Throwable cause) {
			super(cause);
		}

		public FormsFeederDataException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
