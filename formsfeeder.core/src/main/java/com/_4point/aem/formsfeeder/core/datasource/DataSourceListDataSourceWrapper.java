package com._4point.aem.formsfeeder.core.datasource;

public class DataSourceListDataSourceWrapper extends DataSourceWrapper {

	private final ZipDataSourceWrapper zip;
	
	private DataSourceListDataSourceWrapper(DataSource dataSource) {
		super(dataSource);
		this.zip = ZipDataSourceWrapper.wrap(dataSource);
	}
	
	public static DataSourceListDataSourceWrapper wrap(DataSource dataSource) {
		return new DataSourceListDataSourceWrapper(dataSource);
	}

	public DataSourceList asDataSourceList() {
		return null;
	}
}
