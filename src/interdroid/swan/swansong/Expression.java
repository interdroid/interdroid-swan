package interdroid.swan.swansong;


public interface Expression extends Parseable<Expression> {

	/**
	 * Separator used by the SWAN framework in internal ids. This should not be
	 * used in expression ids.
	 */
	public static final String SEPARATOR = "~REMOTE~";

	public static final String LEFT_SUFFIX = ".left";
	public static final String RIGHT_SUFFIX = ".right";

	public static final String[] RESERVED_SUFFIXES = { LEFT_SUFFIX,
			RIGHT_SUFFIX };

	// There are two special locations: local (on the device itself) and
	// independent (doesn't matter where)
	public static final String LOCATION_SELF = "self";
	public static final String LOCATION_INDEPENDENT = "independent";
	public static final String LOCATION_INFER = "infer";
	public static final String REGID_PREFIX = "regid:";

	public void setInferredLocation(String location);

	public String getLocation();

}
