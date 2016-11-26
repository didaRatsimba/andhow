package yarnandtail.andhow.load;

import yarnandtail.andhow.LoaderException;
import yarnandtail.andhow.Loader;
import java.util.HashMap;
import java.util.Map;
import yarnandtail.andhow.AppConfig;
import yarnandtail.andhow.ConfigPoint;
//import yarnandtail.andhow.*;

/**
 *
 * @author eeverman
 */
public class CmdLineLoader extends BaseLoader {

	
	@Override
	public Map<ConfigPoint<?>, Object> load(LoaderState state) {
		
		Map<ConfigPoint<?>, Object> values = new HashMap();
			
		for (String s : state.getCmdLineArgs()) {
			try {
				KVP kvp = KVP.splitKVP(s, AppConfig.KVP_DELIMITER);

				attemptToPut(state, values, kvp.getName(), kvp.getValue());
				
			} catch (ParsingException e) {
				state.getLoaderExceptions().add(
						new LoaderException(e, this, null, "Command line parameters")
				);
			}
		}
		
		return values;
	}
	
	
	
}
