package formsfeeder.client.cli.parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AppParameters {

	public HostParameters hostParameters();

	public Optional<String> contextRoot();

	public Map<String, List<String>> queryParams();

	public Optional<AuthParameters> authParameters();

	public Optional<Map<String,String>> headers();
	
	public List<DataSourceInfo> dataSourceInfos();

	Optional<Path> output();

	public String plugin();
	
	public boolean verbose();
}
