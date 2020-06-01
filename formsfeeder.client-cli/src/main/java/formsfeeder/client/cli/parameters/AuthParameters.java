package formsfeeder.client.cli.parameters;

public abstract class AuthParameters {
	
	public static class BasicAuthParameters extends AuthParameters {

		private final String username;
		private final String password;

		/**
		 * @param username
		 * @param password
		 */
		public BasicAuthParameters(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}

		public final String username() {
			return username;
		}

		public final String password() {
			return password;
		}

	}

}
