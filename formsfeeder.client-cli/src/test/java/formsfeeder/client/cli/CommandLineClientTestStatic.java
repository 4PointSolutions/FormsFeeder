package formsfeeder.client.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;
import com._4point.aem.formsfeeder.core.datasource.MimeType;
import com._4point.aem.formsfeeder.core.datasource.StandardMimeTypes;

class CommandLineClientTestStatic {
	private static final Path RESOURCES_FOLDER = Paths.get("src", "test", "resources");
	private static final Path SAMPLE_FILES_DIR = RESOURCES_FOLDER.resolve("SampleFiles");
	private static final Path ACTUAL_RESULTS_DIR = RESOURCES_FOLDER.resolve("ActualResults");
	private static final Path SAMPLE_XDP = SAMPLE_FILES_DIR.resolve("SampleForm.xdp");
	private static final Path SAMPLE_PDF = SAMPLE_FILES_DIR.resolve("SampleForm.pdf");
	private static final Path SAMPLE_DATA = SAMPLE_FILES_DIR.resolve("SampleForm_data.xml");

	// DataSource Names
	private static final String BOOLEAN_DS_NAME = "BooleanDS";
	private static final String BYTE_ARRAY_DS_NAME = "ByteArrayDS";
	private static final String DOUBLE_DS_NAME = "DoubleDS";
	private static final String FLOAT_DS_NAME = "FloatDS";
	private static final String INTEGER_DS_NAME = "IntegerDS";
	private static final String LONG_DS_NAME = "LongDS";
	private static final String FILE_DS_NAME = "FileDS";
	private static final String STRING_DS_NAME = "StringDS";
	private static final String DUMMY_DS_NAME = "DummyDS";
	private static final String BYTE_ARRAY_W_CT_DS_NAME = "ByteArrayDSWithContentType";
	
	// Custom Data Source
	private static final DataSource dummyDS = new DataSource() {
		byte[] data = "Some Data".getBytes();
		
		@Override
		public OutputStream outputStream() {
			return null;
		}
		
		@Override
		public String name() {
			return DUMMY_DS_NAME;
		}
		
		@Override
		public InputStream inputStream() {
			return new ByteArrayInputStream(data);
		}
		
		@Override
		public Optional<Path> filename() {
			return Optional.empty();
		}
		
		@Override
		public MimeType contentType() {
			return StandardMimeTypes.APPLICATION_OCTET_STREAM_TYPE;
		}
		
		@Override
		public Map<String, String> attributes() {
			return Collections.emptyMap();
		}
	};

	// Data for standard data sources.
	private static final boolean booleanData = true;
	private static final byte[] byteArrayData = new byte[0];
	private static final double doubleData = Double.MAX_VALUE;
	private static final float floatData = Float.MAX_VALUE;
	private static final int intData = Integer.MAX_VALUE;
	private static final long longData = Long.MAX_VALUE;
	private static final Path pathData = SAMPLE_XDP;
	private static final String stringData = "String Data";
	private static final MimeType mimeType = StandardMimeTypes.APPLICATION_PDF_TYPE;


	@Test
	void testWriteDataSourceListToZip() throws Exception {
		DataSourceList input = DataSourceList.builder()
				.add(dummyDS)
				.add(BOOLEAN_DS_NAME, booleanData)
				.add(BYTE_ARRAY_DS_NAME, byteArrayData)
				.add(DOUBLE_DS_NAME, doubleData)
				.add(FLOAT_DS_NAME, floatData)
				.add(INTEGER_DS_NAME, intData)
				.add(LONG_DS_NAME, longData)
				.add(FILE_DS_NAME, pathData)
				.add(STRING_DS_NAME, stringData)
				.add(BYTE_ARRAY_W_CT_DS_NAME, byteArrayData, mimeType)
				.build();
		
		try (var out = Files.newOutputStream(ACTUAL_RESULTS_DIR.resolve("WriteDataSourceListToZip_results.zip"))) {
			CommandLineClient.writeDataSourceListToZip(input, out);
		}
	}

}
