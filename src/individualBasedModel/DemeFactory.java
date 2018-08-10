package individualBasedModel;

import io.Parameter;

import java.util.List;

/**
 * factory class to generate demes with parameters
 * @author Samantha Lycett
 * @created 4 July 2013
 * @version 4 July 2013
 */
public class DemeFactory {

	public static Deme getDeme(List<Parameter> params) {
		
		Deme deme = null;
		
		if (params.get(0).getParentTag().equals("Deme")) {
			deme = new Deme();
			deme.setDemeParameters(params);
		}
		
		return deme;
	}
	
	/**
	 * 6 Aug 14 - EBH - Added so that Deme has link to Population it's in, so that deme
	 * can call function that gets a random deme from the deme groups in the pop
	 * @param params
	 * @param thisPop
	 * @return
	 */
	public static Deme getDeme(List<Parameter> params, Population thisPop) {
		
		Deme deme = null;
		
		if (params.get(0).getParentTag().equals("Deme")) {
			deme = new Deme();
			deme.setDemeParameters(params);
			deme.setPopulation(thisPop);
		}
		
		return deme;
	}
	
}
