package formsfeeder.client.cli.parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface AppParameters {

	public String hostLocation();

	public Optional<AuthParameters> authParameters();
	
	public List<DataSourceInfo> dataSourceInfos();

	Optional<Path> output();
	
	public boolean verbose();
}
