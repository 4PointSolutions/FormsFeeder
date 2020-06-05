package formsfeeder.client.cli.writer;

import java.nio.file.Path;
import java.util.Objects;

import com._4point.aem.formsfeeder.core.datasource.DataSource;
import com._4point.aem.formsfeeder.core.datasource.DataSourceList;

/**
 * Class for determining the type of Output we will be producing.  We will either be writing to stdout or to
 * a file.
 *
 */
public class OutputType {

	public enum Type {
		USE_STDOUT, USE_FILENAME;
	}

	private final static OutputType USE_STDOUT = new OutputType();
	
	private final Type type;
	private final String filename;
	/**
	 * @param type
	 * @param filename
	 */
	private OutputType(String filename) {
		super();
		this.type = Type.USE_FILENAME;
		this.filename = Objects.requireNonNull(filename, "Filename cannot be null!");
	}
	private OutputType() {
		super();
		this.type = Type.USE_STDOUT;
		this.filename = null;
	}
	public final Type type() {
		return type;
	}
	public final String filename() {
		if (type == Type.USE_STDOUT) {
			throw new UnsupportedOperationException("Can't retrieve a filename when using stdout.");
			
		}
		return filename;
	}
	
	// No output parameter was provided
	public static OutputType from(DataSourceList dsl) {
		int size = dsl.list().size();
		if (size != 1) {
			return USE_STDOUT;	// Multiple results and no output param provided, then use stdout.
		} else {
			// If there's only one result, then use filename if it's there.
			DataSource dataSource = dsl.list().get(0);
			return dataSource.filename()
							 .map((f)->f.getFileName().toString())		// Get just the filename part
							 .map((f)->new OutputType(f))				// Convert to OutputType with that filename
							 .orElse(USE_STDOUT);
		}
	}
	
	// An output parameter *was* provided
	public static OutputType from(DataSourceList dsl, Path outputParam ) {
		String theFilename = outputParam.getFileName().toString();
		if (theFilename.equals("---")) {
			return USE_STDOUT;
		} else {
			return new OutputType(theFilename);
		}
	}
}
