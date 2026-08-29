package me.farmador.animepics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer2D;
import org.rusherhack.client.api.render.graphic.TextureGraphic;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.notification.NotificationType;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.setting.StringSetting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnimePicsModule extends ToggleableModule {

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0";

	private static final List<String> WAIFU_NSFW_CYCLE_LIST = Arrays.asList(
			"ero", "ecchi", "oppai", "hentai", "milf", "ass", "paizuri", "oral"
	);

	private static final List<String> PURRBOT_NSFW_LIST = Arrays.asList(
			"fuck", "blowjob", "cum", "anal", "pussylick", "solo", "yaoi", "yuri", "neko"
	);

	private final Random random = new Random();

	// Fonte Principal (Somente NSFW & LocalFolder)
	private final EnumSetting<Source> source = new EnumSetting<>("Source", Source.YandeRE);

	// Yande.re Settings
	private final EnumSetting<BooruRating> yandeRating = new EnumSetting<>("YandeRating", BooruRating.Explicit);
	private final StringSetting yandeCustomTag = new StringSetting("YandeTag", "");
	private final BooleanSetting yandeRandomPage = new BooleanSetting("YandeRandomPage", true);

	// Konachan Settings (Somente rating NSFW)
	private final EnumSetting<BooruRating> konachanRating = new EnumSetting<>("KonachanRating", BooruRating.Explicit);
	private final StringSetting konachanCustomTag = new StringSetting("KonachanTag", "");
	private final BooleanSetting konachanRandomPage = new BooleanSetting("KonachanRandomPage", true);

	// PurrBot (GIFs NSFW) Settings
	private final EnumSetting<PurrBotNsfwTag> purrbotNsfwTag = new EnumSetting<>("PurrNsfwTag", PurrBotNsfwTag.blowjob);
	private final BooleanSetting cyclePurrbot = new BooleanSetting("CyclePurrBot", true);

	// Waifu.im Settings (Somente tags NSFW)
	private final EnumSetting<WaifuNsfwTag> waifuTag = new EnumSetting<>("WaifuTag", WaifuNsfwTag.ero);
	private final BooleanSetting cycleWaifu = new BooleanSetting("CycleWaifu", true);

	// Posição & Tamanho
	private final NumberSetting<Integer> xPos = new NumberSetting<>("X", 4, 0, 3840);
	private final NumberSetting<Integer> yPos = new NumberSetting<>("Y", 4, 0, 2160);
	private final NumberSetting<Integer> imgWidth = new NumberSetting<>("Width", 240, 50, 1200);
	private final NumberSetting<Integer> imgHeight = new NumberSetting<>("Height", 240, 50, 1200);

	// Atualização e Download Automático
	private final BooleanSetting pauseRefresh = new BooleanSetting("PauseRefresh", false);
	private final NumberSetting<Integer> delay = new NumberSetting<>("Delay", 600, 60, 12000);
	private final BooleanSetting autoDownload = new BooleanSetting("AutoDownload", false);

	// GIF Settings
	private final BooleanSetting animateGifs = new BooleanSetting("AnimateGifs", true);
	private final NumberSetting<Integer> maxGifFrames = new NumberSetting<>("MaxGifFrames", 150, 2, 500);

	// Estado Interno
	private final AtomicBoolean locked = new AtomicBoolean(false);
	private boolean empty = true;
	private int ticks = 0;

	private final List<TextureFrame> activeFrames = new ArrayList<>();
	private volatile List<RawFrame> pendingUpload = null;

	private int gifFrameIndex = 0;
	private long lastFrameTime = 0;

	private int waifuCycleIndex = 0;
	private int purrbotCycleIndex = 0;

	private final List<File> localImageFiles = new ArrayList<>();
	private int localImageIndex = 0;

	public AnimePicsModule() {
		super("AnimePics", "Displays random NSFW anime pictures & GIFs on screen", ModuleCategory.RENDER);

		// Visibilidade dinâmica para as fontes
		this.yandeRating.setVisibility(() -> this.source.getValue() == Source.YandeRE);
		this.yandeCustomTag.setVisibility(() -> this.source.getValue() == Source.YandeRE);
		this.yandeRandomPage.setVisibility(() -> this.source.getValue() == Source.YandeRE);

		this.konachanRating.setVisibility(() -> this.source.getValue() == Source.Konachan);
		this.konachanCustomTag.setVisibility(() -> this.source.getValue() == Source.Konachan);
		this.konachanRandomPage.setVisibility(() -> this.source.getValue() == Source.Konachan);

		this.purrbotNsfwTag.setVisibility(() -> this.source.getValue() == Source.PurrBot);
		this.cyclePurrbot.setVisibility(() -> this.source.getValue() == Source.PurrBot);

		this.waifuTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.cycleWaifu.setVisibility(() -> this.source.getValue() == Source.WaifuIM);

		this.registerSettings(
				this.source,
				// Yande.re
				this.yandeRating,
				this.yandeCustomTag,
				this.yandeRandomPage,
				// Konachan
				this.konachanRating,
				this.konachanCustomTag,
				this.konachanRandomPage,
				// PurrBot
				this.purrbotNsfwTag,
				this.cyclePurrbot,
				// Waifu.im
				this.waifuTag,
				this.cycleWaifu,
				// Posição & Tamanho
				this.xPos,
				this.yPos,
				this.imgWidth,
				this.imgHeight,
				// Atualização & AutoDownload
				this.pauseRefresh,
				this.delay,
				this.autoDownload,
				this.animateGifs,
				this.maxGifFrames
		);
	}

	@Override
	public void onEnable() {
		this.empty = true;
		this.ticks = 0;
		this.loadImage();
	}

	@Override
	public void onDisable() {
		this.clearTextures();
	}

	@Subscribe
	private void onUpdate(EventUpdate event) {
		if (mc.level == null) {
			return;
		}

		List<RawFrame> pending = this.pendingUpload;
		if (pending != null) {
			this.clearTextures();
			for (RawFrame raw : pending) {
				TextureGraphic graphic = toGraphic(raw.image);
				if (graphic != null) {
					this.activeFrames.add(new TextureFrame(graphic, raw.delayMs));
				}
			}
			this.empty = this.activeFrames.isEmpty();
			this.gifFrameIndex = 0;
			this.lastFrameTime = System.currentTimeMillis();
			this.pendingUpload = null;
		}

		if (this.source.getValue() == Source.LocalFolder) {
			this.loadLocalFileList();
			if (this.localImageFiles.isEmpty()) {
				return;
			}
		}

		if (this.pauseRefresh.getValue()) {
			return;
		}

		this.ticks++;
		if (this.empty || this.ticks >= this.delay.getValue()) {
			this.ticks = 0;
			this.loadImage();
		}
	}

	@Subscribe
	private void onRender2D(EventRender2D event) {
		if (this.empty || this.activeFrames.isEmpty()) {
			return;
		}

		TextureFrame currentFrame = this.activeFrames.get(this.gifFrameIndex);

		if (this.activeFrames.size() > 1 && this.animateGifs.getValue()) {
			long now = System.currentTimeMillis();
			if (now - this.lastFrameTime >= currentFrame.delay) {
				this.gifFrameIndex = (this.gifFrameIndex + 1) % this.activeFrames.size();
				this.lastFrameTime = now;
				currentFrame = this.activeFrames.get(this.gifFrameIndex);
			}
		}

		double boxX = this.xPos.getValue();
		double boxY = this.yPos.getValue();
		double boxW = this.imgWidth.getValue();
		double boxH = this.imgHeight.getValue();

		IRenderer2D renderer = RusherHackAPI.getRenderer2D();
		renderer.begin(event.getMatrixStack());
		renderer.drawGraphicRectangle(currentFrame.graphic, boxX, boxY, boxW, boxH);
		renderer.end();
	}

	private void clearTextures() {
		this.activeFrames.clear();
		this.empty = true;
	}

	private void loadLocalFileList() {
		File dir = new File(mc.gameDirectory, "rusherhack" + File.separator + "animepics");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		this.localImageFiles.clear();
		File[] files = dir.listFiles((d, name) -> {
			String lower = name.toLowerCase();
			return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif");
		});
		if (files != null) {
			this.localImageFiles.addAll(Arrays.asList(files));
		}
	}

	private void saveImageLocally(byte[] rawBytes, String urlStr) {
		try {
			File downloadsDir = new File(mc.gameDirectory, "rusherhack" + File.separator + "animepics" + File.separator + "downloads");
			if (!downloadsDir.exists()) {
				downloadsDir.mkdirs();
			}

			String ext = ".jpg";
			if (GifDecoder.isGif(rawBytes)) {
				ext = ".gif";
			} else if (urlStr.toLowerCase().endsWith(".png")) {
				ext = ".png";
			} else if (urlStr.toLowerCase().endsWith(".jpeg")) {
				ext = ".jpeg";
			}

			String fileName = "anime_" + System.currentTimeMillis() + "_" + (random.nextInt(9000) + 1000) + ext;
			File outFile = new File(downloadsDir, fileName);
			try (FileOutputStream fos = new FileOutputStream(outFile)) {
				fos.write(rawBytes);
			}
		} catch (Exception ignored) {
		}
	}

	private void loadImage() {
		if (!this.locked.compareAndSet(false, true)) {
			return;
		}

		new Thread(() -> {
			try {
				String urlStr = this.fetchImageUrl();
				if (urlStr == null) {
					return;
				}

				byte[] rawBytes;
				if (urlStr.startsWith("local://")) {
					if (this.localImageFiles.isEmpty()) {
						return;
					}
					File file = this.localImageFiles.get(this.localImageIndex);
					this.localImageIndex = (this.localImageIndex + 1) % this.localImageFiles.size();
					rawBytes = Files.readAllBytes(file.toPath());
				} else {
					rawBytes = downloadBytes(urlStr);
					if (rawBytes != null && rawBytes.length > 0 && this.autoDownload.getValue()) {
						saveImageLocally(rawBytes, urlStr);
					}
				}

				if (rawBytes == null || rawBytes.length == 0) {
					return;
				}

				if (GifDecoder.isGif(rawBytes)) {
					List<RawFrame> decoded = GifDecoder.decode(rawBytes, this.maxGifFrames.getValue());
					if (!decoded.isEmpty()) {
						this.pendingUpload = decoded;
					}
				} else {
					BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawBytes));
					if (img != null) {
						List<RawFrame> list = new ArrayList<>();
						list.add(new RawFrame(img, 100));
						this.pendingUpload = list;
					}
				}
			} catch (Exception ignored) {
			} finally {
				this.locked.set(false);
			}
		}, "AnimePics-Loader").start();
	}

	private String fetchImageUrl() {
		try {
			switch (this.source.getValue()) {
				case YandeRE -> {
					String tags = "";
					String rVal = this.yandeRating.getValue().param;
					if (!rVal.isEmpty()) {
						tags += rVal;
					}
					String custom = this.yandeCustomTag.getValue().trim();
					if (!custom.isEmpty()) {
						if (!tags.isEmpty()) tags += "+";
						tags += URLEncoder.encode(custom, StandardCharsets.UTF_8);
					}
					int page = this.yandeRandomPage.getValue() ? (this.random.nextInt(40) + 1) : 1;
					String url = "https://yande.re/post.json?limit=25&page=" + page;
					if (!tags.isEmpty()) {
						url += "&tags=" + tags;
					}

					HttpURLConnection conn = open(url);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonElement root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
					if (!root.isJsonArray()) return null;
					JsonArray posts = root.getAsJsonArray();
					if (posts.isEmpty()) return null;
					JsonObject chosen = posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
					if (chosen.has("sample_url") && !chosen.get("sample_url").isJsonNull()) {
						return chosen.get("sample_url").getAsString();
					}
					if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
						return chosen.get("file_url").getAsString();
					}
					return null;
				}

				case Konachan -> {
					String tags = "";
					String rVal = this.konachanRating.getValue().param;
					if (!rVal.isEmpty()) {
						tags += rVal;
					}
					String custom = this.konachanCustomTag.getValue().trim();
					if (!custom.isEmpty()) {
						if (!tags.isEmpty()) tags += "+";
						tags += URLEncoder.encode(custom, StandardCharsets.UTF_8);
					}
					int page = this.konachanRandomPage.getValue() ? (this.random.nextInt(40) + 1) : 1;
					String url = "https://konachan.com/post.json?limit=25&page=" + page;
					if (!tags.isEmpty()) {
						url += "&tags=" + tags;
					}

					HttpURLConnection conn = open(url);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonElement root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
					if (!root.isJsonArray()) return null;
					JsonArray posts = root.getAsJsonArray();
					if (posts.isEmpty()) return null;
					JsonObject chosen = posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
					if (chosen.has("sample_url") && !chosen.get("sample_url").isJsonNull()) {
						return chosen.get("sample_url").getAsString();
					}
					if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
						return chosen.get("file_url").getAsString();
					}
					return null;
				}

				case PurrBot -> {
					String tag;
					if (this.cyclePurrbot.getValue()) {
						tag = PURRBOT_NSFW_LIST.get(this.purrbotCycleIndex);
						this.purrbotCycleIndex = (this.purrbotCycleIndex + 1) % PURRBOT_NSFW_LIST.size();
					} else {
						tag = this.purrbotNsfwTag.getValue().name();
					}

					String url = "https://purrbot.site/api/img/nsfw/" + tag + "/gif";
					HttpURLConnection conn = open(url);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonObject pRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					if (pRes.has("link") && !pRes.get("link").isJsonNull()) {
						return pRes.get("link").getAsString();
					}
					return null;
				}

				case WaifuIM -> {
					String tag = this.cycleWaifu.getValue()
							? WAIFU_NSFW_CYCLE_LIST.get(this.waifuCycleIndex)
							: this.waifuTag.getValue().name();
					if (this.cycleWaifu.getValue()) {
						this.waifuCycleIndex = (this.waifuCycleIndex + 1) % WAIFU_NSFW_CYCLE_LIST.size();
					}

					String wUrl = "https://api.waifu.im/images?included_tags=" + URLEncoder.encode(tag, StandardCharsets.UTF_8) + "&is_nsfw=true";
					HttpURLConnection conn = open(wUrl);
					if (conn.getResponseCode() != 200) {
						return null;
					}

					JsonObject wRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					JsonArray items = wRes.has("items") ? wRes.getAsJsonArray("items") : wRes.getAsJsonArray("images");
					if (items == null || items.isEmpty()) {
						return null;
					}

					JsonObject item = items.get(this.random.nextInt(items.size())).getAsJsonObject();
					if (item.has("url") && !item.get("url").isJsonNull()) {
						return item.get("url").getAsString();
					}
					return null;
				}

				case LocalFolder -> {
					return "local://image";
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static HttpURLConnection open(String urlString) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", "application/json, image/*, */*");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(15000);
		return conn;
	}

	private static byte[] downloadBytes(String urlStr) throws Exception {
		HttpURLConnection conn = open(urlStr);
		try (InputStream in = conn.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			byte[] data = new byte[16384];
			int nRead;
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			return buffer.toByteArray();
		}
	}

	private static TextureGraphic toGraphic(BufferedImage image) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return new TextureGraphic(new ByteArrayInputStream(out.toByteArray()), image.getWidth(), image.getHeight());
		} catch (Exception e) {
			return null;
		}
	}

	public enum Source {
		YandeRE,
		Konachan,
		PurrBot,
		WaifuIM,
		LocalFolder
	}

	public enum BooruRating {
		Explicit("rating:explicit"),
		Questionable("rating:questionable");

		public final String param;

		BooruRating(String param) {
			this.param = param;
		}
	}

	public enum PurrBotNsfwTag {
		fuck, blowjob, cum, anal, pussylick, solo, yaoi, yuri, neko
	}

	public enum WaifuNsfwTag {
		ero, ecchi, oppai, hentai, milf, ass, paizuri, oral
	}

	static class RawFrame {
		final BufferedImage image;
		final int delayMs;

		RawFrame(BufferedImage image, int delayMs) {
			this.image = image;
			this.delayMs = delayMs;
		}
	}

	private static class TextureFrame {
		final TextureGraphic graphic;
		final int delay;

		TextureFrame(TextureGraphic graphic, int delay) {
			this.graphic = graphic;
			this.delay = delay;
		}
	}
}
