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

	public static void send(String webhookUrl, ImageMetadata meta, boolean debug) {
		if (webhookUrl == null) return;
		String cleanUrl = webhookUrl.trim();
		if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
			if (debug) {
				System.out.println("[AnimePics Debug] Discord Webhook URL is invalid or empty: '" + webhookUrl + "'");
			}
			return;
		}

		new Thread(() -> {
			HttpURLConnection conn = null;
			try {
				if (debug) {
					System.out.println("[AnimePics Debug] Preparing Discord Webhook embed for post: " + (meta.postUrl != null ? meta.postUrl : meta.url));
				}

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

				embed.addProperty("color", 0xFF1493);

				if (meta.url != null && !meta.url.startsWith("local://")) {
					JsonObject imageObj = new JsonObject();
					imageObj.addProperty("url", meta.url);
					embed.add("image", imageObj);
				}

				JsonArray fields = new JsonArray();

				JsonObject siteField = new JsonObject();
				siteField.addProperty("name", "🌐 Source");
				siteField.addProperty("value", meta.sourceSite != null ? meta.sourceSite : "Unknown");
				siteField.addProperty("inline", true);
				fields.add(siteField);

				if (meta.author != null && !meta.author.trim().isEmpty()) {
					JsonObject authorField = new JsonObject();
					authorField.addProperty("name", "👤 Artist / Author");
					authorField.addProperty("value", meta.author);
					authorField.addProperty("inline", true);
					fields.add(authorField);
				}

				JsonObject ratingField = new JsonObject();
				ratingField.addProperty("name", "🔞 Rating");
				ratingField.addProperty("value", meta.rating != null ? meta.rating : "Explicit / NSFW");
				ratingField.addProperty("inline", true);
				fields.add(ratingField);

				if (meta.width > 0 && meta.height > 0) {
					JsonObject resField = new JsonObject();
					resField.addProperty("name", "📐 Resolution");
					resField.addProperty("value", meta.width + "x" + meta.height);
					resField.addProperty("inline", true);
					fields.add(resField);
				}

				if (meta.sourceOrigin != null && !meta.sourceOrigin.trim().isEmpty() && !meta.sourceOrigin.equals("null")) {
					JsonObject originField = new JsonObject();
					originField.addProperty("name", "🔗 Original Post / Source");
					originField.addProperty("value", meta.sourceOrigin);
					originField.addProperty("inline", false);
					fields.add(originField);
				}

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

				sendJsonPayload(cleanUrl, root, debug);
			} catch (Exception e) {
				System.err.println("[AnimePics] Failed to send Discord Webhook: " + e.getMessage());
				if (debug) {
					e.printStackTrace();
				}
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		}, "AnimePics-DiscordWebhook").start();
	}

	public static void sendNutEvent(String webhookUrl, JoiManager.NutResult nutResult, ImageMetadata meta, JoiStats allStats, boolean debug) {
		if (webhookUrl == null) return;
		String cleanUrl = webhookUrl.trim();
		if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
			return;
		}

		new Thread(() -> {
			try {
				JsonObject root = new JsonObject();
				root.addProperty("username", "AnimePics JOI Companion");
				root.addProperty("avatar_url", "https://i.imgur.com/83pL6rX.png");

				JsonObject embed = new JsonObject();
				embed.addProperty("title", "💦 Nut Milestone Logged! [" + nutResult.score().rank() + " | " + nutResult.score().title() + "]");
				embed.addProperty("description", "A player just recorded a climax session! Here are the full statistics and performance evaluation.");
				embed.addProperty("color", nutResult.score().colorHex() & 0xFFFFFF);

				if (meta != null && meta.url != null && !meta.url.startsWith("local://")) {
					JsonObject imageObj = new JsonObject();
					imageObj.addProperty("url", meta.url);
					embed.add("image", imageObj);
				}

				JsonArray fields = new JsonArray();

				// Duração da Sessão
				long m = nutResult.durationSeconds() / 60;
				long s = nutResult.durationSeconds() % 60;
				String durStr = (m > 0 ? m + "m " : "") + s + "s";

				JsonObject durField = new JsonObject();
				durField.addProperty("name", "⏱️ Session Time");
				durField.addProperty("value", "`" + durStr + "`");
				durField.addProperty("inline", true);
				fields.add(durField);

				// Rank e Classificação
				JsonObject rankField = new JsonObject();
				rankField.addProperty("name", "🏆 Rating & Score");
				rankField.addProperty("value", "**Rank " + nutResult.score().rank() + "** - " + nutResult.score().title() + "\n*" + nutResult.score().description() + "*");
				rankField.addProperty("inline", true);
				fields.add(rankField);

				// Edges Conquistados
				JsonObject edgesField = new JsonObject();
				edgesField.addProperty("name", "🧗 Edges in Session");
				edgesField.addProperty("value", "`" + nutResult.sessionEdges() + " edges`");
				edgesField.addProperty("inline", true);
				fields.add(edgesField);

				// Total Acumulado de Nuts
				JsonObject totalField = new JsonObject();
				totalField.addProperty("name", "📈 All-Time Nuts");
				totalField.addProperty("value", "**#" + nutResult.allTimeNuts() + "** total");
				totalField.addProperty("inline", true);
				fields.add(totalField);

				// Recordes
				if (allStats != null) {
					long fastM = allStats.fastestNutSeconds / 60;
					long fastS = allStats.fastestNutSeconds % 60;
					String fastStr = (fastM > 0 ? fastM + "m " : "") + fastS + "s";

					long longM = allStats.longestNutSeconds / 60;
					long longS = allStats.longestNutSeconds % 60;
					String longStr = (longM > 0 ? longM + "m " : "") + longS + "s";

					JsonObject recordsField = new JsonObject();
					recordsField.addProperty("name", "⚡ Records (Fastest / Longest)");
					recordsField.addProperty("value", "⚡ Fastest: `" + fastStr + "`\n💎 Longest: `" + longStr + "`");
					recordsField.addProperty("inline", true);
					fields.add(recordsField);
				}

				// Imagem Ativa no Climax
				if (meta != null) {
					JsonObject artField = new JsonObject();
					artField.addProperty("name", "🖼️ Climax Artwork");
					String artText = "Source: **" + meta.sourceSite + "**";
					if (meta.author != null && !meta.author.isEmpty()) {
						artText += " by *" + meta.author + "*";
					}
					if (meta.postUrl != null && !meta.postUrl.isEmpty()) {
						artText += " • [View Post](" + meta.postUrl + ")";
					}
					artField.addProperty("value", artText);
					artField.addProperty("inline", false);
					fields.add(artField);
				}

				// Nota do Usuário (se houver)
				if (nutResult.record().note != null && !nutResult.record().note.isEmpty()) {
					JsonObject noteField = new JsonObject();
					noteField.addProperty("name", "📝 Player Note");
					noteField.addProperty("value", "> " + nutResult.record().note);
					noteField.addProperty("inline", false);
					fields.add(noteField);
				}

				embed.add("fields", fields);

				JsonObject footer = new JsonObject();
				footer.addProperty("text", "AnimePics JOI & Nut Tracker • Keep Striving!");
				embed.add("footer", footer);

				JsonArray embeds = new JsonArray();
				embeds.add(embed);
				root.add("embeds", embeds);

				sendJsonPayload(cleanUrl, root, debug);
			} catch (Exception e) {
				System.err.println("[AnimePics] Failed to send Nut Event Webhook: " + e.getMessage());
				if (debug) {
					e.printStackTrace();
				}
			}
		}, "AnimePics-NutWebhook").start();
	}

	public static void sendEdgeEvent(String webhookUrl, int sessionEdges, int allTimeEdges, long durationSec, boolean debug) {
		if (webhookUrl == null) return;
		String cleanUrl = webhookUrl.trim();
		if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
			return;
		}

		new Thread(() -> {
			try {
				JsonObject root = new JsonObject();
				root.addProperty("username", "AnimePics JOI Companion");
				root.addProperty("avatar_url", "https://i.imgur.com/83pL6rX.png");

				JsonObject embed = new JsonObject();
				embed.addProperty("title", "🧗 Edge Milestone #" + sessionEdges + " Reached!");
				embed.addProperty("description", "A player successfully brought themselves to the brink and held back!");
				embed.addProperty("color", 0xFF007F);

				long m = durationSec / 60;
				long s = durationSec % 60;

				JsonArray fields = new JsonArray();

				JsonObject sessionField = new JsonObject();
				sessionField.addProperty("name", "⏱️ Session Time");
				sessionField.addProperty("value", "`" + m + "m " + s + "s`");
				sessionField.addProperty("inline", true);
				fields.add(sessionField);

				JsonObject edgesField = new JsonObject();
				edgesField.addProperty("name", "🧗 Total Edges");
				edgesField.addProperty("value", "`" + sessionEdges + " this session` (`" + allTimeEdges + " all-time`)");
				edgesField.addProperty("inline", true);
				fields.add(edgesField);

				embed.add("fields", fields);

				JsonArray embeds = new JsonArray();
				embeds.add(embed);
				root.add("embeds", embeds);

				sendJsonPayload(cleanUrl, root, debug);
			} catch (Exception e) {
				if (debug) {
					e.printStackTrace();
				}
			}
		}, "AnimePics-EdgeWebhook").start();
	}

	private static void sendJsonPayload(String cleanUrl, JsonObject root, boolean debug) throws Exception {
		byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);

		HttpURLConnection conn = (HttpURLConnection) URI.create(cleanUrl).toURL().openConnection();
		SSLHelper.configure(conn);

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
		if (code >= 200 && code < 300) {
			if (debug) {
				System.out.println("[AnimePics Debug] Discord Webhook payload delivered! (HTTP " + code + ")");
			}
		} else {
			StringBuilder err = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					err.append(line);
				}
			} catch (Exception ignored) {
			}
			System.err.println("[AnimePics] Discord Webhook Error HTTP " + code + ": " + err);
		}
		conn.disconnect();
	}
}
