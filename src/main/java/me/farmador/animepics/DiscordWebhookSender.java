package me.farmador.animepics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookSender {

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

	public static void send(String webhookUrl, ImageMetadata meta) {
		if (webhookUrl == null) return;
		String cleanUrl = webhookUrl.trim();
		if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
			return;
		}

		new Thread(() -> {
			HttpURLConnection conn = null;
			try {
				JsonObject root = new JsonObject();
				root.addProperty("username", "AnimePics RusherHack");
				root.addProperty("avatar_url", "https://i.imgur.com/83pL6rX.png");

				JsonObject embed = new JsonObject();
				embed.addProperty("title", "🔞 NSFW Anime Artwork (" + meta.sourceSite + ")");
				if (meta.postUrl != null && !meta.postUrl.isEmpty()) {
					embed.addProperty("url", meta.postUrl);
				} else if (meta.url != null && !meta.url.startsWith("local://")) {
					embed.addProperty("url", meta.url);
				}

				// Cor rosa/roxa sexy para o embed (#FF1493)
				embed.addProperty("color", 0xFF1493);

				// Imagem em destaque
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
				JsonObject ratingField = new JsonObject();
				ratingField.addProperty("name", "🔞 Rating");
				ratingField.addProperty("value", meta.rating != null ? meta.rating : "Explicit / NSFW");
				ratingField.addProperty("inline", true);
				fields.add(ratingField);

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
					originField.addProperty("name", "🔗 Original Post / Source");
					originField.addProperty("value", meta.sourceOrigin);
					originField.addProperty("inline", false);
					fields.add(originField);
				}

				// Campo: Tags
				if (meta.tags != null && !meta.tags.isEmpty()) {
					StringBuilder tagBuilder = new StringBuilder();
					int count = 0;
					for (String t : meta.tags) {
						if (count++ >= 18) {
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
				footer.addProperty("text", "RusherHack AnimePics Plugin • NSFW Feed");
				embed.add("footer", footer);

				JsonArray embeds = new JsonArray();
				embeds.add(embed);
				root.add("embeds", embeds);

				byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);

				conn = (HttpURLConnection) URI.create(cleanUrl).toURL().openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", USER_AGENT);
				conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				conn.setRequestProperty("Accept", "application/json");
				conn.setDoOutput(true);
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);

				try (OutputStream os = conn.getOutputStream()) {
					os.write(payload);
					os.flush();
				}

				int code = conn.getResponseCode();
				if (code < 200 || code >= 300) {
					// Leitura de erro para diagnóstico caso ocorra
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()))) {
						StringBuilder err = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							err.append(line);
						}
						System.err.println("[AnimePics] Discord Webhook Error HTTP " + code + ": " + err);
					}
				}
			} catch (Exception e) {
				System.err.println("[AnimePics] Failed to send Discord Webhook: " + e.getMessage());
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		}, "AnimePics-DiscordWebhook").start();
	}
}
