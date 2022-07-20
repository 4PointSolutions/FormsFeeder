package com._4point.aem.formsfeeder.plugins.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com._4point.aem.formsfeeder.core.api.NamedFeedConsumer;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Builder;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList.Deconstructor;

/**
 * Mock plugin is used by the unit testing code to test various plug-in behaviours and scenarios relating to submitting data from an form. 
 *
 */
@Component
@Extension
public class MockSubmitExtension implements NamedFeedConsumer, ExtensionPoint {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String FEED_CONSUMER_NAME = "MockSubmit";
	
	private static final String SCENARIO_NAME_UNKNOWN = "Unknown";

	// List of valid scenarios
	private static final String SCENARIO_NAME_BAD_REQUEST_EXCEPTION = "BadRequestException";
	private static final String SCENARIO_NAME_INTERNAL_ERROR_EXCEPTION = "InternalErrorException";
	private static final String SCENARIO_NAME_UNCHECKED_EXCEPTION = "UncheckedException";
	private static final String SCENARIO_OTHER_FEED_CONSUMER_EXCEPTION = "OtherFeedConsumerException";
	private static final String SCENARIO_RETURN_PDF = "ReturnPdf";
	private static final String SCENARIO_NO_RETURNS = "NoReturns";
	private static final String SCENARIO_RETURN_HTML5_PARAMS = "ReturnHtml5Parameters";
	private static final String SCENARIO_RETURN_REDIRECT = "ReturnRedirect";
	private static final String SCENARIO_RETURN_TOOMANY = "ReturnTooMany";

	// Other constants
	private static final String FORMSFEEDER_PREFIX = "formsfeeder:";
	private static final String REDIRECT_LOCATION_DS_NAME = FORMSFEEDER_PREFIX + "RedirectLocation";
	
	private FileSystem zipfs = null;	// Used to hold ZipFs so that we can read our .jar resources using FileSystem
	
	public MockSubmitExtension() {
		super();
		logger.debug("inside MockSubmitExtension constructor");
	}

	@SuppressWarnings("serial")
	@Override
	public DataSourceList accept(DataSourceList dataSources) throws FeedConsumerException {
		
		InputParameters iparams = InputParameters.from(dataSources); 
		Builder builder = DataSourceList.builder();
		
		String scenario = iparams.scenario().orElse(SCENARIO_NAME_UNKNOWN);	// retrieve the unit testing scenario
		logger.info("MockSubmitPlugin scenario is {}", scenario);
		switch(scenario)
		{
		// TODO Need to implement various scenarios.
		case SCENARIO_NO_RETURNS:
			break;
		case SCENARIO_RETURN_PDF:
			// This scenario returns a single PDF file.
			builder.add("PdfResult", getResourcePath("SampleForm.pdf"), Map.of("formsfeeder:Content-Disposition", "attachment"));
			break;
		case SCENARIO_RETURN_HTML5_PARAMS:
			// This scenario returns a String with HTML5 Parameters.
			String returnValue = String.join(", ",
												iparams.templateUrl().map(s->"TemplateUrl='" + s + "'").orElse("No Template Url"),
												iparams.contentRoot().map(s->"ContentRoot='" + s + "'").orElse("No Content Root"),
												iparams.submitUrl().map(s->"SubmitUrl='" + s + "'").orElse("No Submit Url")
											);
			builder.add("Result", returnValue);
			break;
		case SCENARIO_RETURN_REDIRECT:
			builder.add(REDIRECT_LOCATION_DS_NAME, "RedirectUrl");
			break;
		case SCENARIO_OTHER_FEED_CONSUMER_EXCEPTION:
			// This scenario returns some new kind of FeedConsumerException.
			throw new FeedConsumerException() {

				@Override
				public String getMessage() {
					return "Throwing anonymous FeedConsumerException because scenario was '" + scenario + "'.";
				}
				
			};
		case SCENARIO_RETURN_TOOMANY:
			// This scenario returns a single PDF file.
			builder.add("PdfResult", getResourcePath("SampleForm.pdf"), Map.of("formsfeeder:Content-Disposition", "attachment"));
			String returnValue2 = String.join(", ",
					iparams.templateUrl().map(s->"TemplateUrl='" + s + "'").orElse("No Template Url"),
					iparams.contentRoot().map(s->"ContentRoot='" + s + "'").orElse("No Content Root"),
					iparams.submitUrl().map(s->"SubmitUrl='" + s + "'").orElse("No Submit Url")
				);
			builder.add("Result", returnValue2);
			break;
		case SCENARIO_NAME_BAD_REQUEST_EXCEPTION:
			// This scenarion returns a FeedConsumerBadRequestException.
			throw new FeedConsumerBadRequestException("Throwing FeedConsumerBadRequestException because scenario was '" + scenario + "'.");
		case SCENARIO_NAME_INTERNAL_ERROR_EXCEPTION:
			// This scenarion returns a FeedConsumerInternalErrorException.
			throw new FeedConsumerInternalErrorException("Throwing FeedConsumerInternalErrorException because scenario was '" + scenario + "'.");
		case SCENARIO_NAME_UNCHECKED_EXCEPTION:
			// This scenarion returns an exception not derived from FeedConsumerException.
			throw new IllegalStateException("Throwing IllegalStateException because scenario was '" + scenario + "'.");
		case SCENARIO_NAME_UNKNOWN:
			// This should never happen and only happens if the caller doesn't provide a scenario in the input Dsl.
			throw new FeedConsumerBadRequestException("No scenario name was provided.");
		default:
			// This should never happen and only happens if the caller provides a scenario that this plugin does not recognize.
			throw new FeedConsumerBadRequestException("Unexpected scenario name was provided (" + scenario + ").");
		}
		
		return builder.build();
	}

	@Override
	public String name() {
		return FEED_CONSUMER_NAME;
	}

	private static class InputParameters {
		private static final String SUBMITTED_DATA_DS_NAME = FORMSFEEDER_PREFIX + "SubmittedData";
		private static final String TEMPLATE_DS_NAME = FORMSFEEDER_PREFIX + "Template";
		private static final String CONTENT_ROOT_DS_NAME = FORMSFEEDER_PREFIX + "ContentRoot";
		private static final String SUBMIT_URL_DS_NAME = FORMSFEEDER_PREFIX + "SubmitUrl";

		private final Deconstructor deconstructor;
		
		private InputParameters(Deconstructor deconstructor) {
			super();
			this.deconstructor = deconstructor;
		}

		public static InputParameters from(DataSourceList dataSources) {
			return new InputParameters(dataSources.deconstructor());
		}
		
		Optional<String> scenario() {
			return deconstructor.getByteArrayByName(SUBMITTED_DATA_DS_NAME).flatMap(InputXmlData::from).flatMap(InputXmlData::scenario);
		}
		
		Optional<String> templateUrl() {
			return deconstructor.getStringByName(TEMPLATE_DS_NAME);
		}
		
		Optional<String> contentRoot() {
			return deconstructor.getStringByName(CONTENT_ROOT_DS_NAME);
		}
		
		Optional<String> submitUrl() {
			return deconstructor.getStringByName(SUBMIT_URL_DS_NAME);
		}
	}
	
	private static class InputXmlData {
		private static final String SCENARIO_ELEMENT =  "Scenario";
		private static final String SCENARIO_XPATH_STRING = "//" + SCENARIO_ELEMENT;	// String containing XPath to FormsFeeder element 

		private static final XPath XPATH_PROCESSOR = XPathFactory.newInstance().newXPath();
		private static final XPathExpression SCENARIO_XPATH;
		static {
			try {
				SCENARIO_XPATH = XPATH_PROCESSOR.compile(SCENARIO_XPATH_STRING);
			} catch (XPathExpressionException e) {
				throw new IllegalStateException("Unable to initialize XPaths.", e);
			}
		}

		private final Document xmlDom;

		private InputXmlData(Document xmlDom) {
			super();
			this.xmlDom = xmlDom;
		}
		
		public Optional<String> scenario() {
			try {
				String name = SCENARIO_XPATH.evaluate(xmlDom);
				name = name != null ? name.strip() : name;	// Strip blanks off the name
				return (name == null || name.isBlank()) ? Optional.empty() : Optional.of(name);
			} catch (XPathExpressionException e) {
				throw new InputXmlDataException("Unable to locate scenario name.", e);
			}
		}

		public static Optional<InputXmlData> from(byte[] xmlData) {
			return from(new ByteArrayInputStream(xmlData));
		}
		
		public static Optional<InputXmlData> from(InputStream is) {
			Document xmlDoc;
			try {
				xmlDoc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(is);
			} catch (SAXException | IOException | ParserConfigurationException e) {
				throw new InputXmlDataException(e);
			}
			return from(xmlDoc);
		}
		
		public static Optional<InputXmlData> from(Document xmlDoc) {
			return Optional.ofNullable(new InputXmlData(xmlDoc));
		}

		@SuppressWarnings({"serial", "unused"})
		public static class InputXmlDataException extends RuntimeException {

			public InputXmlDataException() {
			}

			public InputXmlDataException(String message) {
				super(message);
			}

			public InputXmlDataException(Throwable cause) {
				super(cause);
			}

			public InputXmlDataException(String message, Throwable cause) {
				super(message, cause);
			}
		}

	}

	private final Path getResourcePath(String resourceName) throws FeedConsumerInternalErrorException {
		URL jarResource = getClass().getClassLoader().getResource(resourceName);
		if (jarResource == null) {
			throw new FeedConsumerInternalErrorException("Problem locating pdf Resource.");
		} else {
			try {
				URI uri = jarResource.toURI();
				if (zipfs == null && (uri.toString().startsWith("/") || uri.toString().startsWith("jar:"))) {
					try {
						zipfs = FileSystems.getFileSystem(uri);
					} catch (FileSystemNotFoundException e) {
						// File system doesn't exist, so create it.
						zipfs = FileSystems.newFileSystem(uri, Map.of("create", "true"));
					}
					return Path.of(uri);
				} else {
					return Path.of(uri);
				}
			} catch (URISyntaxException | IOException e) {
				throw new FeedConsumerInternalErrorException("Problem with converting jar resource to path.", e);
			}
		}
	}

}
