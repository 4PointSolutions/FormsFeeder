package formsfeeder.client.cli.parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class DataSourceInfo {
	
	public enum Type { PATH, STRING };

	public abstract Type type();

	public abstract Path path();
	
	public abstract String value();
	
	public static DataSourceInfo from(String param) {
		if (param.startsWith("@")) {
			return new PathDataSourceInfo(Paths.get(param.substring(1)));
		} else {
			return new StringDataSourceInfo(param);
		}
	}
	
	private static class PathDataSourceInfo extends DataSourceInfo {

		private final Path path;
		
		private PathDataSourceInfo(Path path) {
			super();
			this.path = path;
		}

		@Override
		public Type type() {
			return Type.PATH;
		}

		@Override
		public Path path() {
			return path;
		}

		@Override
		public String value() {
			throw new UnsupportedOperationException("Can't get the value of a PathDataSourceInfo.");
		}
	}
	
	private static class StringDataSourceInfo extends DataSourceInfo {
		private final String value;

		private StringDataSourceInfo(String value) {
			super();
			this.value = value;
		}

		@Override
		public Type type() {
			return Type.STRING;
		}

		@Override
		public Path path() {
			throw new UnsupportedOperationException("Can't get the path of a StringDataSourceInfo.");
		}

		@Override
		public String value() {
			return value;
		}
		
	}
}
