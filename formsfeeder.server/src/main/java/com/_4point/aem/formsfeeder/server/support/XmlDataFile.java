package com._4point.aem.formsfeeder.server.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.ws.rs.BadRequestException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class XmlDataFile {
	private static final XPath XPATH_PROCESSOR = XPathFactory.newInstance().newXPath();
	private static final String FORM_NODE_PATH_STR = "//xfa:data[1]/node()[1]";
//	private static final String DATA_IDENTITY_STYLESHEET = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\r\n" + 
//			"  <xsl:template match=\"@*|node()\">\r\n" + 
//			"    <xsl:copy>\r\n" + 
//			"      <xsl:apply-templates select=\"@*|node()\"/>\r\n" + 
//			"    </xsl:copy>\r\n" + 
//			"  </xsl:template>\r\n" + 
//			"</xsl:stylesheet>";
	
	private static final String DATA_EXTRACTION_STYLESHEET = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xfa=\"http://www.xfa.org/schema/xfa-data/1.0/\">\r\n" +
			"<xsl:strip-space elements=\"*\"/>" +
			"<xsl:template match=\"/\">\r\n" + 
			"        <xsl:copy-of select=\"" + FORM_NODE_PATH_STR + "\" copy-namespaces=\"no\">\r\n" + 	// Select the first node under xfa:data (this is always the form's data)
			"        </xsl:copy-of>\r\n" + 
			"    </xsl:template>" +
			"</xsl:stylesheet>";
	private static final XPathExpression FORM_NODE_XPATH;
	static {
		try {
			XPATH_PROCESSOR.setNamespaceContext(new NamespaceContext() {
			    public String getNamespaceURI(String prefix) {
			        return prefix.equals("xfa") ? "http://www.xfa.org/schema/xfa-data/1.0/" : null;
			    }

			    public Iterator<String> getPrefixes(String val) {
			        return null;
			    }

			    public String getPrefix(String uri) {
			        return null;
			    }
			});
			FORM_NODE_XPATH = XPATH_PROCESSOR.compile(FORM_NODE_PATH_STR);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Unable to initialize XPaths.", e);
		}
	}

	/**
	 * Use XSLT to pull the form data out of the incoming data stream (i.e. remove the xdp:xdp, xfa:datasets and xfa:data wrappers).
	 * 
	 * @param data
	 * @param logger
	 * @return
	 * @throws ParserConfigurationException
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws TransformerException 
	 * @throws XPathExpressionException 
	 */
	public static byte[] extractData(byte[] data, Logger logger) throws ParserConfigurationException {
		return extractData(new ByteArrayInputStream(data), logger).readAllBytes();
	}
	
	/**
	 * Use XSLT to pull the form data out of the incoming data stream (i.e. remove the xdp:xdp, xfa:datasets and xfa:data wrappers).
	 * 
	 * @param is
	 * @param logger 
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws TransformerException 
	 * @throws XPathExpressionException 
	 */
	public static ByteArrayInputStream extractData(InputStream is, Logger logger) throws ParserConfigurationException {
		try {
			// Load the data into a DOM so that we can perform a query on it.
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
			factory.setNamespaceAware(true);
			factory.setIgnoringElementContentWhitespace(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final org.w3c.dom.Document document = builder.parse(is);
			
			// Perform query to determine if a xfa:data node is present.  If not then throw an exception.
			// This is to ensure that we don't end up returning an empty XML file.
			String formNode = FORM_NODE_XPATH.evaluate(document);
			if (formNode == null || formNode.isEmpty()) {
				final String msg = "Incoming data is invalid.  It does not have a form node within an xfa:data node.";
				logger.error(msg);
				throw new BadRequestException(msg);
			}
			
			// Now that we know there's an xfa:data node, then we can safely extract the data from within that node.
			DOMSource dataSource = new DOMSource(document);
			StreamSource styleSource = new StreamSource(new ByteArrayInputStream(DATA_EXTRACTION_STYLESHEET.getBytes(StandardCharsets.UTF_8)));
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			StreamResult resultStream = new StreamResult(result);
			final Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer(styleSource);
			transformer.transform(dataSource, resultStream);
			resultStream.getOutputStream().close();
			return new ByteArrayInputStream(result.toByteArray());
		} catch (XPathExpressionException | SAXException | IOException | TransformerException e) {
			// Catch the exceptions that relate to a bad input stream so return Bad Request status code, 
			// let the other ones go by so that we return Internal Server Error status code. 
			String msg = "Error while parsing the incoming data stream.";
			logger.error(msg, e);
			throw new BadRequestException(msg, e);
		}
	}
	

	@SuppressWarnings("serial")
	public static class XmlDataFileException extends Exception {

		public XmlDataFileException() {
			super();
		}

		public XmlDataFileException(String message, Throwable cause) {
			super(message, cause);
		}

		public XmlDataFileException(String message) {
			super(message);
		}

		public XmlDataFileException(Throwable cause) {
			super(cause);
		}
	}
}
