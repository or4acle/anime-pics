package me.farmador.animepics;

import com.google.gson.JsonArray;
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
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.setting.StringSetting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

	private static final List<String> NEKOS_CYCLE_LIST = Arrays.asList(
			"neko", "waifu", "fox_girl", "hug", "kiss", "meow", "lizard", "goose", "gecg",
			"avatar", "feed", "cuddle", "woof", "smug", "tickle", "slap", "pat", "wallpaper"
	);

	private static final List<String> WAIFU_CYCLE_LIST = Arrays.asList(
			"waifu", "maid", "uniform", "genshin-impact", "raiden-shogun",
			"marin-kitagawa", "mori-calliope", "kamisato-ayaka",
			"ero", "ecchi", "oppai", "hentai", "milf", "ass", "paizuri", "oral"
	);

	private final EnumSetting<Source> source = new EnumSetting<>("Source", Source.WaifuIM);
	private final EnumSetting<NekosTag> nekosCategory = new EnumSetting<>("NekosCategory", NekosTag.neko);
	private final BooleanSetting cycleNekos = new BooleanSetting("CycleNekos", true);
	private final EnumSetting<WaifimTag> waifuTag = new EnumSetting<>("WaifuTag", WaifimTag.waifu);
	private final BooleanSetting cycleWaifu = new BooleanSetting("CycleWaifu", true);
	private final EnumSetting<NsfwMode> nsfwMode = new EnumSetting<>("NsfwMode", NsfwMode.All);
	private final StringSetting safebooruTag = new StringSetting("SafebooruTag", "solo");

	private final NumberSetting<Integer> xPos = new NumberSetting<>("X", 2, 0, 1920);
	private final NumberSetting<Integer> yPos = new NumberSetting<>("Y", 2, 0, 1080);
	private final NumberSetting<Integer> imgWidth = new NumberSetting<>("Width", 200, 50, 800);
	private final NumberSetting<Integer> imgHeight = new NumberSetting<>("Height", 200, 50, 800);

	private final BooleanSetting pauseRefresh = new BooleanSetting("PauseRefresh", false);
	private final NumberSetting<Integer> refreshRate = new NumberSetting<>("RefreshRate", 1200, 100, 72000);
	private final BooleanSetting animateGifs = new BooleanSetting("AnimateGifs", true);
	private final NumberSetting<Integer> maxGifFrames = new NumberSetting<>("MaxGifFrames", 150, 2, 500);

	private final AtomicBoolean locked = new AtomicBoolean(false);
	private boolean empty = true;
	private int ticks = 0;

	private final List<TextureFrame> activeFrames = new ArrayList<>();
	private volatile List<RawFrame> pendingUpload = null;

	private int gifFrameIndex = 0;
	private long lastFrameTime = 0;

	private int nekosCycleIndex = 0;
	private int waifuCycleIndex = 0;

	private final List<File> localImageFiles = new ArrayList<>();
	private int localImageIndex = 0;

	public AnimePicsModule() {
		super("AnimePics", "Displays random anime pictures/GIFs (NSFW compatible)", ModuleCategory.RENDER);

		this.nekosCategory.setVisibility(() -> this.source.getValue() == Source.NekosLife);
		this.cycleNekos.setVisibility(() -> this.source.getValue() == Source.NekosLife);
		this.waifuTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.cycleWaifu.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.nsfwMode.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.safebooruTag.setVisibility(() -> this.source.getValue() == Source.Safebooru);

		this.registerSettings(
				this.source,
				this.nekosCategory,
				this.cycleNekos,
				this.waifuTag,
				this.cycleWaifu,
				this.nsfwMode,
				this.safebooruTag,
				this.xPos,
				this.yPos,
				this.imgWidth,
				this.imgHeight,
				this.pauseRefresh,
				this.refreshRate,
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
			this.pendingUpload = null;
			this.empty = this.activeFrames.isEmpty();
			this.gifFrameIndex = 0;
			this.lastFrameTime = System.currentTimeMillis();
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
		if (this.empty || this.ticks >= this.refreshRate.getValue()) {
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

		IRenderer2D renderer = RusherHackAPI.getRenderer2D();
		renderer.begin(event.getMatrixStack());
		renderer.drawGraphicRectangle(
				currentFrame.graphic,
				this.xPos.getValue(),
				this.yPos.getValue(),
				this.imgWidth.getValue(),
				this.imgHeight.getValue()
		);
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
				case NekosLife -> {
					String category = this.cycleNekos.getValue()
							? NEKOS_CYCLE_LIST.get(this.nekosCycleIndex)
							: this.nekosCategory.getValue().name();
					if (this.cycleNekos.getValue()) {
						this.nekosCycleIndex = (this.nekosCycleIndex + 1) % NEKOS_CYCLE_LIST.size();
					}
					return getJsonUrl("https://nekos.life/api/v2/img/" + category, "url");
				}
				case WaifuIM -> {
					String tag = this.cycleWaifu.getValue()
							? WAIFU_CYCLE_LIST.get(this.waifuCycleIndex)
							: this.waifuTag.getValue().name().replace('_', '-');
					if (this.cycleWaifu.getValue()) {
						this.waifuCycleIndex = (this.waifuCycleIndex + 1) % WAIFU_CYCLE_LIST.size();
					}

					String nsfwParam = this.nsfwMode.getValue().paramValue;
					String wUrl = "https://api.waifu.im/images?"
							+ "included_tags=" + URLEncoder.encode(tag, StandardCharsets.UTF_8);
					if (!"All".equalsIgnoreCase(nsfwParam)) {
						wUrl += "&is_nsfw=" + nsfwParam;
					}
					HttpURLConnection conn = open(wUrl);
					conn.setRequestProperty("Accept", "application/json");
					if (conn.getResponseCode() != 200) {
						this.getLogger().error("Waifu API error: HTTP " + conn.getResponseCode() + " for tag: " + tag);
						return null;
					}

					JsonObject wRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					JsonArray items = wRes.has("items") ? wRes.getAsJsonArray("items") : wRes.getAsJsonArray("images");
					if (items == null || items.size() == 0) {
						return null;
					}
					return items.get(new Random().nextInt(items.size())).getAsJsonObject().get("url").getAsString();
				}
				case Safebooru -> {
					String encoded = URLEncoder.encode(this.safebooruTag.getValue(), StandardCharsets.UTF_8);
					int pid = new Random().nextInt(700);
					String sUrl = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&tags="
							+ encoded + "&limit=10&pid=" + pid;
					HttpURLConnection sConn = open(sUrl);
					if (sConn.getResponseCode() != 200) {
						this.getLogger().error("Safebooru API error: HTTP " + sConn.getResponseCode());
						return null;
					}

					JsonArray sArr = JsonParser.parseReader(new InputStreamReader(sConn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonArray();
					if (sArr.size() == 0) {
						return null;
					}
					JsonObject post = sArr.get(new Random().nextInt(sArr.size())).getAsJsonObject();
					if (post.has("file_url")) {
						return post.get("file_url").getAsString();
					}
					if (post.has("preview_url")) {
						return post.get("preview_url").getAsString();
					}
					return "https://safebooru.org/images/" + post.get("directory").getAsString() + "/" + post.get("image").getAsString();
				}
				case LocalFolder -> {
					return "local://";
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private String getJsonUrl(String urlString, String key) throws Exception {
		HttpURLConnection conn = open(urlString);
		JsonObject response = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
		return response.get(key).getAsString();
	}

	private static HttpURLConnection open(String urlString) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0");
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
		NekosLife, WaifuIM, Safebooru, LocalFolder
	}

	public enum NekosTag {
		neko, waifu, fox_girl, hug, kiss, meow, gecg, avatar, feed, cuddle, woof, smug, tickle, slap, pat, wallpaper
	}

	public enum WaifimTag {
		waifu, maid, uniform, genshin_impact, raiden_shogun, marin_kitagawa, mori_calliope, kamisato_ayaka,
		ero, ecchi, oppai, hentai, milf, ass, paizuri, oral
	}

	public enum NsfwMode {
		All("All"),
		Only("true"),
		None("false");

		public final String paramValue;

		NsfwMode(String paramValue) {
			this.paramValue = paramValue;
		}
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
