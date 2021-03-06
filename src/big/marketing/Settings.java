package big.marketing;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Settings {
	static Logger logger = Logger.getLogger(Settings.class);

	private static Properties properties = new Properties();
	private static final String configFile = "config/eyeNet.properties";
	private static final String defaultConfigFile = configFile + ".template";

	public static String get(String key) {
		return properties.getProperty(key);
	}

	public static int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	public static void loadConfig() {

		boolean defaultConfigOK = loadConfigFile(defaultConfigFile);

		// this enables hierarchical properties:
		// eyeNet.properties doesn't have to contain all settings, only the ones different from default
		// settings not existing in eyeNet.properties are getting the default value
		Properties defaults = properties;
		properties = new Properties(defaults);

		boolean userConfigOK = loadConfigFile(configFile);

		if (!userConfigOK) {

			if (!defaultConfigOK) {
				logger.fatal("Could not load any config, exiting...");
				System.exit(1);
			} else {
				// no user config file existing, saving default config as user config
				try {
					defaults.store(new FileWriter(configFile), "");
				} catch (IOException e) {
					logger.warn("Could not write config file to " + configFile);
				}
			}

		}

	}

	public static void setAndSave(String key, String value) {
		properties.setProperty(key, value);
		try {
			properties.store(new FileWriter(configFile), "");
		} catch (IOException e) {
			logger.error("Could not write config file");
		}
	}

	private static boolean loadConfigFile(String fileName) {
		try {
			properties.load(new FileReader(fileName));
			logger.info("Successfully read config from file " + fileName);
			return true;
		} catch (IOException e) {
			logger.warn("Could not load Settings from " + fileName);
			return false;
		}
	}

}
