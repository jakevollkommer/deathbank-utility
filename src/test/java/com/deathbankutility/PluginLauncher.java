package com.deathbankutility;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.slf4j.LoggerFactory;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		// Dev launches only (test sources are not in the published jar): the plugin's
		// diagnostics are debug level, and the client defaults to info
		((Logger) LoggerFactory.getLogger("com.deathbankutility")).setLevel(Level.DEBUG);

		ExternalPluginManager.loadBuiltin(DeathbankUtilityPlugin.class);
		RuneLite.main(args);
	}
}
