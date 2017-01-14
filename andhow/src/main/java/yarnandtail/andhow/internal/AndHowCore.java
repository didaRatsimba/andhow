package yarnandtail.andhow.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import yarnandtail.andhow.*;
import yarnandtail.andhow.PropertyValue;
import yarnandtail.andhow.load.FixedValueLoader;
import yarnandtail.andhow.name.BasicNamingStrategy;
import yarnandtail.andhow.util.ReportGenerator;

/**
 * Actual central instance of the AndHow state after a successful startup.
 * The advertised AndHow class is really a proxy for this class, and allows
 * interaction with the AndHow framework prior to startup, reloading during unit
 * testing, and (potentially) a future implementation where reloading of production
 * data would be allowed.
 * 
 * @author eeverman
 */
public class AndHowCore implements ValueMap {
	//User config
	private final ArrayList<PropertyValue> forcedValues = new ArrayList();
	private final ArrayList<PropertyValue> defaultValues = new ArrayList();
	private final List<Loader> loaders = new ArrayList();
	private final NamingStrategy namingStrategy;
	private final List<String> cmdLineArgs = new ArrayList();
	
	//Internal state
	private final ConstructionDefinition runtimeDef;
	private final ValueMapWithContext loadedValues;
	private final List<ConstructionProblem> constructProblems = new ArrayList();
	private final ArrayList<LoaderProblem> loaderProblems = new ArrayList();
	private final ArrayList<RequirementProblem> requirementsProblems = new ArrayList();
	
	public AndHowCore(NamingStrategy naming, List<Loader> loaders, 
			List<Class<? extends PropertyGroup>> registeredGroups, 
			String[] cmdLineArgs, List<PropertyValue> forceValues, List<PropertyValue> defaultValues) throws AppFatalException {
		
		this.namingStrategy = (naming != null)?naming:new BasicNamingStrategy();
		
		if (loaders != null) {
			for (Loader loader : loaders) {
				if (! this.loaders.contains(loader)) {
					this.loaders.add(loader);
				} else {
					constructProblems.add(new ConstructionProblem.DuplicateLoader(loader));
				}
			}
		}
		
		if (forceValues != null) {
			this.forcedValues.addAll(forceValues);
			this.forcedValues.trimToSize();
		}
		
		if (defaultValues != null) {
			this.defaultValues.addAll(defaultValues);
			this.defaultValues.trimToSize();
		}
		
		if (cmdLineArgs != null && cmdLineArgs.length > 0) {
			this.cmdLineArgs.addAll(Arrays.asList(cmdLineArgs));
		}

		ConstructionDefinitionMutable startupDef = AndHowUtil.doRegisterProperties(registeredGroups, loaders, namingStrategy);
		constructProblems.addAll(startupDef.getConstructionProblems());
		runtimeDef = startupDef.toImmutable();
		
		//
		//If there are ConstructionProblems, we can't continue on to attempt to
		//load values.
		if (constructProblems.size() > 0) {
			AppFatalException afe = new AppFatalException(constructProblems);
			printFailedStartupDetails(afe);
			throw afe;
		}
		
		//Continuing on to load values
		loadedValues = loadValues(runtimeDef).getValueMapWithContextImmutable();

		checkForRequiredValues(runtimeDef);

		if (requirementsProblems.size() > 0 || loadedValues.hasProblems() || loaderProblems.size() > 0) {
			AppFatalException afe = AndHowUtil.buildFatalException(loaderProblems, requirementsProblems, loadedValues);
			printFailedStartupDetails(afe);
			throw afe;
		}
	}
	
	private void printFailedStartupDetails(AppFatalException afe) {
		ReportGenerator.printProblems(System.err, afe, runtimeDef);
		ReportGenerator.printConfigSamples(System.err, runtimeDef, loaders, true);
	}

	public List<Class<? extends PropertyGroup>> getPropertyGroups() {
		return runtimeDef.getPropertyGroups();
	}

	public List<Property<?>> getProperties() {
		return runtimeDef.getProperties();
	}
	
	@Override
	public boolean isExplicitlySet(Property<?> prop) {
		return loadedValues.isExplicitlySet(prop);
	}
	
	@Override
	public <T> T getExplicitValue(Property<T> prop) {
		return loadedValues.getExplicitValue(prop);
	}
	
	@Override
	public <T> T getEffectiveValue(Property<T> prop) {
		return loadedValues.getEffectiveValue(prop);
	}
	
	private ValueMapWithContext loadValues(ConstructionDefinition definition) {
		ValueMapWithContextMutable existingValues = new ValueMapWithContextMutable();

		//force values by adding a fixed value loader before all other loaders
		if (forcedValues.size() > 0) {
			FixedValueLoader fvl = new FixedValueLoader(forcedValues);
			loaders.add(0, fvl);
		}
		
		//Set instance specific default values by adding a fixed loader after all other laoders
		if (defaultValues.size() > 0) {
			FixedValueLoader fvl = new FixedValueLoader(defaultValues);
			loaders.add(fvl);
		}

		//LoaderState state = new LoaderState(cmdLineArgs, existingValues, runtimeDef);
		for (Loader loader : loaders) {
			LoaderValues result = loader.load(definition, cmdLineArgs, existingValues);
			existingValues.addValues(result);
			loaderProblems.addAll(result.getProblems());
		}

		return existingValues;
	}
	

	private void checkForRequiredValues(ConstructionDefinition definition) {
		
		for (Property<?> prop : definition.getProperties()) {
			if (prop.isRequired()) {
				if (getEffectiveValue(prop) == null) {
					
					requirementsProblems.add(
						new RequirementProblem.RequiredPropertyProblem(
								definition.getGroupForProperty(prop), prop));
				}
			}
		}
		
	}
		
}
