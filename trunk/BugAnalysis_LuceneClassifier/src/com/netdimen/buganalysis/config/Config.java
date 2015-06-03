package com.netdimen.buganalysis.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class Config {

	public final static String directoryPath = System.getProperty("user.dir") + "/resource/directory/";

	public final static String dateFormat = "MM-dd-yyyy";

	private Properties allProperties;

	private static final Config instance = new Config();

	public static Config getInstance() {

		return instance;
	}

	public String getProperty(final String key) {

		return allProperties.getProperty(key);
	}

	public void setProperty(final String key, final String value) {

		allProperties.setProperty(key, value);
	}

	private static void loadProperties(final Properties prop, final String sProperties) {

		try {
			final InputStream input = new FileInputStream(sProperties);
			prop.load(input);

			if (!(prop.getProperty("bug.analysis.report.dir") == null)) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("bug.analysis.report.dir", prop.getProperty("bug.analysis.report.dir"));
				map.put("bug.analysis.report.template.sheet", prop.getProperty("bug.analysis.report.template.sheet"));
				map.put("bug.analysis.report.sheet.summary", prop.getProperty("bug.analysis.report.sheet.summary"));
				map.put("bug.analysis.report.sheet.details", prop.getProperty("bug.analysis.report.sheet.details"));
			}
			
			input.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private Config() {

		allProperties = new Properties();
		loadProperties(allProperties, "./conf/config.properties");
	}

}
