package formsfeeder.client.cli.parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class DataSourceInfo {

	private final String name;
	
	private DataSourceInfo(String name) {
		super();
		this.name = name;
	}

	public enum Type { PATH, STRING };

	public String name() {
		return this.name;
	}

	public abstract Type type();

	public abstract Path path();
	
	public abstract String value();
	
	public static DataSourceInfo from(String name, String param) {
		if (param.startsWith("@")) {
			return new PathDataSourceInfo(name, Paths.get(param.substring(1)));
		} else {
			return new StringDataSourceInfo(name, param);
		}
	}
	
	private static class PathDataSourceInfo extends DataSourceInfo {

		private final Path path;
		
		private PathDataSourceInfo(String name, Path path) {
			super(name);
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

		private StringDataSourceInfo(String name, String value) {
			super(name);
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
