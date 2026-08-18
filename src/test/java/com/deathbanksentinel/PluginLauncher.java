package com.deathbanksentinel;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DeathbankSentinelPlugin.class);
		RuneLite.main(args);
	}
}
