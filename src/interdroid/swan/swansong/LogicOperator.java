package interdroid.swan.swansong;

public interface LogicOperator {
	
	public int convert();

	public LogicOperator convertInt(final int val);
		
	/**
	 * Parses and returns a UnaryLogicOperator.
	 * 
	 * @param val
	 *            the string to parse
	 * @return the corresponding UnaryLogicOperator
	 */
	public LogicOperator parseString(final String val);
	
	public TriState operate(TriState first, TriState last);

}
