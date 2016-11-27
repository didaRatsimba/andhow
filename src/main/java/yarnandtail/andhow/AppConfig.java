package yarnandtail.andhow;

import yarnandtail.andhow.appconfig.AppConfigCore;
import java.util.List;
import java.util.Map;

/**
 *
 * @author eeverman
 */
public class AppConfig {
	
	/**
	 * In text formats, this is the default delimiter between a key and a value.
	 * Known usage:  The CmdLineLoader uses this value to parse values.
	 */
	public static final String KVP_DELIMITER = "=";
	
	
	private static AppConfig singleInstance;
	private static final Object lock = new Object();
	
	AppConfigCore core;
	Reloader reloader;
	
	private AppConfig(NamingStrategy naming, List<Loader> loaders, 
			List<Class<? extends ConfigPointGroup>> registeredGroups, 
			String[] cmdLineArgs, Map<ConfigPoint<?>, Object> startingValues)
			throws ConfigurationException {
		core = new AppConfigCore(naming, loaders, registeredGroups, cmdLineArgs, startingValues);
		reloader = new Reloader(this);
	}
	
	public static AppConfig instance() {
		if (singleInstance != null) {
			return singleInstance;
		} else {
			return null;	//this should block
		}
	}
	
	public static Reloader build(
			NamingStrategy naming, List<Loader> loaders, List<Class<? extends ConfigPointGroup>> registeredGroups, 
			String[] cmdLineArgs, Map<ConfigPoint<?>, Object> startingValues) throws ConfigurationException {

		synchronized (lock) {
			if (singleInstance != null) {
				throw new RuntimeException("Already constructed!");
			} else {
				singleInstance = new AppConfig(naming, loaders, registeredGroups, cmdLineArgs, startingValues);
				return singleInstance.reloader;
			}
		}

	}
	
	public List<Class<? extends ConfigPointGroup>> getGroups() {
		return core.getGroups();
	}

	public List<ConfigPoint<?>> getPoints() {
		return core.getPoints();
	}
	
	public boolean isPointPresent(ConfigPoint<?> point) {
		return core.isPointPresent(point);
	}
	
	public Object getValue(ConfigPoint<?> point) {
		return core.getValue(point);
	}


	public static class Reloader {
		private final AppConfig instance;
		
		private Reloader(AppConfig instance) {
			this.instance = instance;
		}
		
		public void reload(NamingStrategy naming, List<Loader> loaders, 
				List<Class<? extends ConfigPointGroup>> registeredGroups, String[] cmdLineArgs, 
				Map<ConfigPoint<?>, Object> forcedValues) 
				throws ConfigurationException {
			
			synchronized (AppConfig.lock) {
				instance.core = new AppConfigCore(naming, loaders, registeredGroups, cmdLineArgs, forcedValues);
			}
		}
	}

}
