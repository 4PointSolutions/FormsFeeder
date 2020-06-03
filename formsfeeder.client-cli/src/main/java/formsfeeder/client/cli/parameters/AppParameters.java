package formsfeeder.client.cli.parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface AppParameters {

	public HostParameters hostParameters();

	public Optional<AuthParameters> authParameters();
	
	public List<DataSourceInfo> dataSourceInfos();

	Optional<Path> output();

	public String plugin();
	
	public boolean verbose();
}
