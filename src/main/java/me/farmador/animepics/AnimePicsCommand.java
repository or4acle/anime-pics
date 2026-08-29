package me.farmador.animepics;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.core.command.annotations.CommandExecutor;

public class AnimePicsCommand extends Command {

	public AnimePicsCommand() {
		super("animepics", "Search tags, change sources, webhook, or skip to next anime picture");
	}

	private AnimePicsModule getModule() {
		IModule module = RusherHackAPI.getModuleManager().getFeature("AnimePics").orElse(null);
		if (module instanceof AnimePicsModule animePicsModule) {
			return animePicsModule;
		}
		return null;
	}

	/**
	 * Base command: .animepics
	 */
	@CommandExecutor
	private String baseCommand() {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		String webhookStatus = module.isWebhookEnabled() ? (module.getWebhookUrl().isEmpty() ? "[Enabled - No URL]" : "[Active]") : "[Disabled]";
		return "AnimePics | Source: " + module.getSource().name() +
				" | Webhook: " + webhookStatus +
				" | YandeTags: '" + module.getYandeTags() + "'" +
				" | KonachanTags: '" + module.getKonachanTags() + "'";
	}

	/**
	 * .animepics next
	 */
	@CommandExecutor(subCommand = "next")
	private String nextImage() {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		module.reloadNow();
		return "Loading next anime picture...";
	}

	/**
	 * .animepics webhook <url>
	 */
	@CommandExecutor(subCommand = "webhook")
	@CommandExecutor.Argument("url")
	private String setWebhook(String url) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}

		if (url.equalsIgnoreCase("off") || url.equalsIgnoreCase("disable") || url.equalsIgnoreCase("false")) {
			module.setWebhookEnabled(false);
			return "Discord Webhook disabled.";
		}

		if (url.equalsIgnoreCase("on") || url.equalsIgnoreCase("enable") || url.equalsIgnoreCase("true")) {
			module.setWebhookEnabled(true);
			return "Discord Webhook enabled! (Current URL: " + (module.getWebhookUrl().isEmpty() ? "None set" : module.getWebhookUrl()) + ")";
		}

		if (url.equalsIgnoreCase("clear")) {
			module.setWebhookUrl("");
			module.setWebhookEnabled(false);
			return "Discord Webhook URL cleared and disabled.";
		}

		module.setWebhookUrl(url.trim());
		module.setWebhookEnabled(true);
		return "Discord Webhook set and enabled! Arts will be sent to your Discord channel.";
	}

	/**
	 * .animepics yande <tags...>
	 */
	@CommandExecutor(subCommand = {"yande", "yandere"})
	@CommandExecutor.Argument("tags")
	private String setYandeTags(String tags) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		module.setSource(AnimePicsModule.Source.YandeRE);
		module.setYandeTags(tags);
		module.reloadNow();
		return "Set Yande.re tags to: [" + tags + "] and loading...";
	}

	/**
	 * .animepics konachan <tags...>
	 */
	@CommandExecutor(subCommand = "konachan")
	@CommandExecutor.Argument("tags")
	private String setKonachanTags(String tags) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		module.setSource(AnimePicsModule.Source.Konachan);
		module.setKonachanTags(tags);
		module.reloadNow();
		return "Set Konachan tags to: [" + tags + "] and loading...";
	}

	/**
	 * .animepics waifu <tag>
	 */
	@CommandExecutor(subCommand = {"waifu", "waifuim"})
	@CommandExecutor.Argument("tag")
	private String setWaifuTag(String tag) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		module.setSource(AnimePicsModule.Source.WaifuIM);
		module.setWaifuTag(tag);
		module.reloadNow();
		return "Set Waifu.im tag to: [" + tag + "] and loading...";
	}

	/**
	 * .animepics purr <tag>
	 */
	@CommandExecutor(subCommand = {"purr", "purrbot"})
	@CommandExecutor.Argument("tag")
	private String setPurrTag(String tag) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		try {
			AnimePicsModule.PurrBotNsfwTag nsfwTag = AnimePicsModule.PurrBotNsfwTag.valueOf(tag.toLowerCase());
			module.setSource(AnimePicsModule.Source.PurrBot);
			module.setPurrbotNsfwTag(nsfwTag);
			module.setCyclePurrbot(false);
			module.reloadNow();
			return "Set PurrBot GIF tag to: [" + nsfwTag.name() + "] and loading...";
		} catch (IllegalArgumentException e) {
			return "Unknown PurrBot tag '" + tag + "'. Valid tags: fuck, blowjob, cum, anal, pussylick, solo, yaoi, yuri, neko";
		}
	}

	/**
	 * .animepics search <tags...> -> applies to current booru / yande.re
	 */
	@CommandExecutor(subCommand = "search")
	@CommandExecutor.Argument("tags")
	private String searchTags(String tags) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		if (module.getSource() == AnimePicsModule.Source.Konachan) {
			module.setKonachanTags(tags);
		} else if (module.getSource() == AnimePicsModule.Source.WaifuIM) {
			module.setWaifuTag(tags);
		} else {
			module.setSource(AnimePicsModule.Source.YandeRE);
			module.setYandeTags(tags);
		}
		module.reloadNow();
		return "Searching tags [" + tags + "] on " + module.getSource().name() + "...";
	}

	/**
	 * .animepics source <sourceName>
	 */
	@CommandExecutor(subCommand = "source")
	@CommandExecutor.Argument("sourceName")
	private String setSource(String sourceName) {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		for (AnimePicsModule.Source s : AnimePicsModule.Source.values()) {
			if (s.name().equalsIgnoreCase(sourceName)) {
				module.setSource(s);
				module.reloadNow();
				return "Switched source to: " + s.name();
			}
		}
		return "Unknown source '" + sourceName + "'. Valid: YandeRE, Konachan, PurrBot, WaifuIM, LocalFolder";
	}

	/**
	 * .animepics clear -> clears tags
	 */
	@CommandExecutor(subCommand = "clear")
	private String clearTags() {
		AnimePicsModule module = getModule();
		if (module == null) {
			return "AnimePics module not found!";
		}
		module.setYandeTags("");
		module.setKonachanTags("");
		module.setWaifuTag("");
		module.reloadNow();
		return "Cleared all search tags and reloaded!";
	}
}
