package me.farmador.animepics;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class AnimePicsPlugin extends Plugin {

	@Override
	public void onLoad() {
		this.getLogger().info("AnimePics loaded");
		RusherHackAPI.getModuleManager().registerFeature(new AnimePicsModule());
	}

	@Override
	public void onUnload() {
		this.getLogger().info("AnimePics unloaded");
	}
}
