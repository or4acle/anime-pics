package me.farmador.animepics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookSender {

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0";

	public static void send(String webhookUrl, ImageMetadata meta) {
		if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("http")) {
			return;
		}

		new Thread(() -> {
			try {
				JsonObject root = new JsonObject();
				root.addProperty("username", "AnimePics RusherHack");
				root.addProperty("avatar_url", "https://i.imgur.com/83pL6rX.png");

				JsonObject embed = new JsonObject();
				embed.addProperty("title", "🎨 New Anime Artwork (" + meta.sourceSite + ")");
				if (meta.postUrl != null && !meta.postUrl.isEmpty()) {
					embed.addProperty("url", meta.postUrl);
				} else if (meta.url != null && !meta.url.startsWith("local://")) {
					embed.addProperty("url", meta.url);
				}

				embed.addProperty("color", 0xE91E63);

				// Imagem principal
				if (meta.url != null && !meta.url.startsWith("local://")) {
					JsonObject imageObj = new JsonObject();
					imageObj.addProperty("url", meta.url);
					embed.add("image", imageObj);
				}

				JsonArray fields = new JsonArray();

				// Campo: Site / Fonte
				JsonObject siteField = new JsonObject();
				siteField.addProperty("name", "🌐 Source");
				siteField.addProperty("value", meta.sourceSite);
				siteField.addProperty("inline", true);
				fields.add(siteField);

				// Campo: Artista / Autor
				if (meta.author != null && !meta.author.trim().isEmpty()) {
					JsonObject authorField = new JsonObject();
					authorField.addProperty("name", "👤 Artist / Author");
					authorField.addProperty("value", meta.author);
					authorField.addProperty("inline", true);
					fields.add(authorField);
				}

				// Campo: Rating
				if (meta.rating != null && !meta.rating.trim().isEmpty()) {
					JsonObject ratingField = new JsonObject();
					ratingField.addProperty("name", "🔞 Rating");
					ratingField.addProperty("value", meta.rating);
					ratingField.addProperty("inline", true);
					fields.add(ratingField);
				}

				// Campo: Resolução
				if (meta.width > 0 && meta.height > 0) {
					JsonObject resField = new JsonObject();
					resField.addProperty("name", "📐 Resolution");
					resField.addProperty("value", meta.width + "x" + meta.height);
					resField.addProperty("inline", true);
					fields.add(resField);
				}

				// Campo: Origem / Link Original
				if (meta.sourceOrigin != null && !meta.sourceOrigin.trim().isEmpty() && !meta.sourceOrigin.equals("null")) {
					JsonObject originField = new JsonObject();
					originField.addProperty("name", "🔗 Original Post / Pixiv");
					originField.addProperty("value", meta.sourceOrigin);
					originField.addProperty("inline", false);
					fields.add(originField);
				}

				// Campo: Tags
				if (meta.tags != null && !meta.tags.isEmpty()) {
					StringBuilder tagBuilder = new StringBuilder();
					int count = 0;
					for (String t : meta.tags) {
						if (count++ > 15) {
							tagBuilder.append("...");
							break;
						}
						tagBuilder.append("`").append(t).append("` ");
					}
					JsonObject tagsField = new JsonObject();
					tagsField.addProperty("name", "🏷️ Tags");
					tagsField.addProperty("value", tagBuilder.toString().trim());
					tagsField.addProperty("inline", false);
					fields.add(tagsField);
				}

				embed.add("fields", fields);

				JsonObject footer = new JsonObject();
				footer.addProperty("text", "RusherHack AnimePics Plugin • Enjoy your waifu");
				embed.add("footer", footer);

				JsonArray embeds = new JsonArray();
				embeds.add(embed);
				root.add("embeds", embeds);

				byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);

				HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl.trim()).toURL().openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", USER_AGENT);
				conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				conn.setRequestProperty("Accept", "application/json");
				conn.setDoOutput(true);
				conn.setConnectTimeout(8000);
				conn.setReadTimeout(8000);

				try (OutputStream os = conn.getOutputStream()) {
					os.write(payload);
					os.flush();
				}

				conn.getResponseCode();
				conn.disconnect();
			} catch (Exception ignored) {
			}
		}, "AnimePics-DiscordWebhook").start();
	}
}
