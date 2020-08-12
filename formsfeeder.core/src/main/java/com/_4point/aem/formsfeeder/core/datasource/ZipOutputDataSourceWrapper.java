package com._4point.aem.formsfeeder.core.datasource;

import static com._4point.aem.formsfeeder.core.datasource.ZipInputDataSourceWrapper.ZipMetadata.encodeMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com._4point.aem.formsfeeder.core.datasource.ZipInputDataSourceWrapper.ZipMetadata.ZipMetadataException;
import com._4point.aem.formsfeeder.core.support.Jdk8Utils;

public class ZipOutputDataSourceWrapper extends DataSourceWrapper {

	private final ZipOutputStream zipStream;
	
	private ZipOutputDataSourceWrapper(DataSource dataSource) {
		super(dataSource);
		this.zipStream = new ZipOutputStream(dataSource.outputStream()); 
	}
	
	public static ZipOutputDataSourceWrapper wrap(DataSource dataSource) {
		return new ZipOutputDataSourceWrapper(dataSource);
	}

	public ZipOutputDataSourceWrapper putNextDataSource(DataSource dataSource) throws IOException, ZipMetadataException {
		ZipEntry zipEntry = new ZipEntry(dataSource.name());
		zipEntry.setComment(encodeMetadata(dataSource.contentType(), dataSource.filename()));
		zipStream.putNextEntry(zipEntry);
		try (InputStream inputStream = dataSource.inputStream()) {
			Jdk8Utils.transfer(inputStream, zipStream);
		}
		zipStream.closeEntry();
		return this;
	}
	
}
