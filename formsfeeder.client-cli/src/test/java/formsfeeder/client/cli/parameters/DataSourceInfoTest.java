package formsfeeder.client.cli.parameters;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DataSourceInfoTest {

	private static final String TEST_DATA = "foo.txt";

	@Test
	void testFromPath() {
		DataSourceInfo underTest = DataSourceInfo.from("@" + TEST_DATA);
		
		// Should create a PathDataSourceList
		assertAll(
				()->assertEquals(DataSourceInfo.Type.PATH, underTest.type()),
				()->assertThrows(UnsupportedOperationException.class, ()->underTest.value()),
				()->assertEquals(TEST_DATA, underTest.path().toString())
				);
		
	}

	@Test
	void testFromString() {
		DataSourceInfo underTest = DataSourceInfo.from(TEST_DATA);

		// Should create a PathDataSourceList
		assertAll(
				()->assertEquals(DataSourceInfo.Type.STRING, underTest.type()),
				()->assertThrows(UnsupportedOperationException.class, ()->underTest.path()),
				()->assertEquals(TEST_DATA, underTest.value())
				);
	}

}
