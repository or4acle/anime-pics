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
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.setting.StringSetting;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnimePicsModule extends ToggleableModule {

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
	private static final String E621_USER_AGENT = "AnimePicsRusherHack/1.0 (by Farmador on e621)";

	// Tags 100% NSFW para Waifu.im
	private static final List<String> WAIFU_NSFW_CYCLE_LIST = Arrays.asList(
			"ero", "ecchi", "oppai", "hentai", "milf", "ass", "paizuri", "oral"
	);

	// Tags 100% NSFW para PurrBot
	private static final List<String> PURRBOT_NSFW_LIST = Arrays.asList(
			"fuck", "blowjob", "cum", "anal", "pussylick", "solo", "yaoi", "yuri", "neko"
	);

	// Tags NSFW complementares para injeção automática e reforço quando o usuário não especificar tags
	private static final List<String> DEFAULT_NSFW_BOORU_TAGS = Arrays.asList(
			"nude", "breasts", "nipples", "pussy", "sex", "panties", "bikini", "lingerie", "cleavage", "ass", "thighs", "ecchi", "swimsuit"
	);

	private final Random random = new Random();
	private final ExecutorService loaderExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "AnimePics-Worker");
		t.setDaemon(true);
		return t;
	});

	// Fonte Principal
	private final EnumSetting<Source> source = new EnumSetting<>("Source", Source.YandeRE);

	// Strict NSFW Enforcer
	private final BooleanSetting strictNSFW = new BooleanSetting("StrictNSFW", true);

	// Aspect Ratio Mode
	private final EnumSetting<AspectMode> aspectMode = new EnumSetting<>("AspectMode", AspectMode.Fit);

	// Yande.re Settings
	private final StringSetting yandeSearchTags = new StringSetting("YandeTags", "");
	private final EnumSetting<BooruRating> yandeRating = new EnumSetting<>("YandeRating", BooruRating.Explicit);
	private final BooleanSetting yandeRandomPage = new BooleanSetting("YandeRandomPage", true);

	// Konachan Settings
	private final StringSetting konachanSearchTags = new StringSetting("KonachanTags", "");
	private final EnumSetting<BooruRating> konachanRating = new EnumSetting<>("KonachanRating", BooruRating.Explicit);
	private final BooleanSetting konachanRandomPage = new BooleanSetting("KonachanRandomPage", true);

	// AIBooru Settings
	private final StringSetting aibooruSearchTags = new StringSetting("AIBooruTags", "");
	private final EnumSetting<BooruRating> aibooruRating = new EnumSetting<>("AIBooruRating", BooruRating.Explicit);
	private final BooleanSetting aibooruRandomPage = new BooleanSetting("AIBooruRandomPage", true);

	// E621 Settings
	private final StringSetting e621SearchTags = new StringSetting("E621Tags", "female");
	private final EnumSetting<BooruRating> e621Rating = new EnumSetting<>("E621Rating", BooruRating.Explicit);
	private final BooleanSetting e621RandomPage = new BooleanSetting("E621RandomPage", true);

	// PurrBot (GIFs NSFW) Settings
	private final EnumSetting<PurrBotNsfwTag> purrbotNsfwTag = new EnumSetting<>("PurrNsfwTag", PurrBotNsfwTag.blowjob);
	private final BooleanSetting cyclePurrbot = new BooleanSetting("CyclePurrBot", true);

	// Waifu.im Settings
	private final StringSetting waifuCustomTag = new StringSetting("WaifuCustomTag", "");
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

	// Discord Webhook Settings
	private final BooleanSetting enableWebhook = new BooleanSetting("EnableWebhook", false);
	private final StringSetting webhookUrl = new StringSetting("WebhookUrl", "");

	// GIF Settings (com otimização anti-lag)
	private final BooleanSetting animateGifs = new BooleanSetting("AnimateGifs", true);
	private final NumberSetting<Integer> maxGifFrames = new NumberSetting<>("MaxGifFrames", 60, 2, 200);

	// Rastreamento para recarregamento instantâneo
	private Source lastSource = null;
	private String lastYandeTags = null;
	private BooruRating lastYandeRating = null;
	private String lastKonachanTags = null;
	private BooruRating lastKonachanRating = null;
	private String lastAibooruTags = null;
	private BooruRating lastAibooruRating = null;
	private String lastE621Tags = null;
	private BooruRating lastE621Rating = null;
	private String lastWaifuTag = null;
	private WaifuNsfwTag lastWaifuEnumTag = null;
	private PurrBotNsfwTag lastPurrTag = null;

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
		this.yandeSearchTags.setVisibility(() -> this.source.getValue() == Source.YandeRE);
		this.yandeRating.setVisibility(() -> this.source.getValue() == Source.YandeRE);
		this.yandeRandomPage.setVisibility(() -> this.source.getValue() == Source.YandeRE);

		this.konachanSearchTags.setVisibility(() -> this.source.getValue() == Source.Konachan);
		this.konachanRating.setVisibility(() -> this.source.getValue() == Source.Konachan);
		this.konachanRandomPage.setVisibility(() -> this.source.getValue() == Source.Konachan);

		this.aibooruSearchTags.setVisibility(() -> this.source.getValue() == Source.AIBooru);
		this.aibooruRating.setVisibility(() -> this.source.getValue() == Source.AIBooru);
		this.aibooruRandomPage.setVisibility(() -> this.source.getValue() == Source.AIBooru);

		this.e621SearchTags.setVisibility(() -> this.source.getValue() == Source.E621);
		this.e621Rating.setVisibility(() -> this.source.getValue() == Source.E621);
		this.e621RandomPage.setVisibility(() -> this.source.getValue() == Source.E621);

		this.purrbotNsfwTag.setVisibility(() -> this.source.getValue() == Source.PurrBot);
		this.cyclePurrbot.setVisibility(() -> this.source.getValue() == Source.PurrBot);

		this.waifuCustomTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.waifuTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.cycleWaifu.setVisibility(() -> this.source.getValue() == Source.WaifuIM);

		this.webhookUrl.setVisibility(() -> this.enableWebhook.getValue());

		this.registerSettings(
				this.source,
				this.strictNSFW,
				this.aspectMode,
				// Yande.re Tag Search & Ratings
				this.yandeSearchTags,
				this.yandeRating,
				this.yandeRandomPage,
				// Konachan Tag Search & Ratings
				this.konachanSearchTags,
				this.konachanRating,
				this.konachanRandomPage,
				// AIBooru
				this.aibooruSearchTags,
				this.aibooruRating,
				this.aibooruRandomPage,
				// E621
				this.e621SearchTags,
				this.e621Rating,
				this.e621RandomPage,
				// PurrBot
				this.purrbotNsfwTag,
				this.cyclePurrbot,
				// Waifu.im
				this.waifuCustomTag,
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
				// Discord Webhook
				this.enableWebhook,
				this.webhookUrl,
				// GIFs
				this.animateGifs,
				this.maxGifFrames
		);
	}

	@Override
	public void onEnable() {
		this.empty = true;
		this.ticks = 0;
		this.initTrackedValues();
		this.loadImage();
	}

	@Override
	public void onDisable() {
		this.clearTextures();
	}

	private void initTrackedValues() {
		this.lastSource = this.source.getValue();
		this.lastYandeTags = this.yandeSearchTags.getValue();
		this.lastYandeRating = this.yandeRating.getValue();
		this.lastKonachanTags = this.konachanSearchTags.getValue();
		this.lastKonachanRating = this.konachanRating.getValue();
		this.lastAibooruTags = this.aibooruSearchTags.getValue();
		this.lastAibooruRating = this.aibooruRating.getValue();
		this.lastE621Tags = this.e621SearchTags.getValue();
		this.lastE621Rating = this.e621Rating.getValue();
		this.lastWaifuTag = this.waifuCustomTag.getValue();
		this.lastWaifuEnumTag = this.waifuTag.getValue();
		this.lastPurrTag = this.purrbotNsfwTag.getValue();
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
				try {
					TextureGraphic graphic = new TextureGraphic(new ByteArrayInputStream(raw.pngBytes), raw.width, raw.height);
					this.activeFrames.add(new TextureFrame(graphic, raw.width, raw.height, raw.delayMs));
				} catch (Exception ignored) {
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

		// Detecção de mudança de configurações para recarregamento instantâneo
		boolean sourceChanged = this.lastSource != this.source.getValue();
		boolean yandeChanged = this.source.getValue() == Source.YandeRE && (!Objects.equals(this.lastYandeTags, this.yandeSearchTags.getValue()) || this.lastYandeRating != this.yandeRating.getValue());
		boolean konachanChanged = this.source.getValue() == Source.Konachan && (!Objects.equals(this.lastKonachanTags, this.konachanSearchTags.getValue()) || this.lastKonachanRating != this.konachanRating.getValue());
		boolean aibooruChanged = this.source.getValue() == Source.AIBooru && (!Objects.equals(this.lastAibooruTags, this.aibooruSearchTags.getValue()) || this.lastAibooruRating != this.aibooruRating.getValue());
		boolean e621Changed = this.source.getValue() == Source.E621 && (!Objects.equals(this.lastE621Tags, this.e621SearchTags.getValue()) || this.lastE621Rating != this.e621Rating.getValue());
		boolean waifuChanged = this.source.getValue() == Source.WaifuIM && (!Objects.equals(this.lastWaifuTag, this.waifuCustomTag.getValue()) || this.lastWaifuEnumTag != this.waifuTag.getValue());
		boolean purrChanged = this.source.getValue() == Source.PurrBot && (this.lastPurrTag != this.purrbotNsfwTag.getValue());

		if (sourceChanged || yandeChanged || konachanChanged || aibooruChanged || e621Changed || waifuChanged || purrChanged) {
			this.initTrackedValues();
			this.ticks = 0;
			this.loadImage();
			return;
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

		double drawX = boxX;
		double drawY = boxY;
		double drawW = boxW;
		double drawH = boxH;

		if (this.aspectMode.getValue() == AspectMode.Fit && currentFrame.width > 0 && currentFrame.height > 0) {
			double ratioImg = (double) currentFrame.width / currentFrame.height;
			double ratioBox = boxW / boxH;
			if (ratioImg > ratioBox) {
				drawW = boxW;
				drawH = boxW / ratioImg;
				drawY = boxY + (boxH - drawH) / 2.0;
			} else {
				drawH = boxH;
				drawW = boxH * ratioImg;
				drawX = boxX + (boxW - drawW) / 2.0;
			}
		}

		IRenderer2D renderer = RusherHackAPI.getRenderer2D();
		renderer.begin(event.getMatrixStack());
		renderer.drawGraphicRectangle(currentFrame.graphic, drawX, drawY, drawW, drawH);
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

	public void reloadNow() {
		this.initTrackedValues();
		this.ticks = 0;
		this.loadImage();
	}

	// Getters and Setters para o Command
	public Source getSource() {
		return this.source.getValue();
	}

	public void setSource(Source source) {
		this.source.setValue(source);
	}

	public String getYandeTags() {
		return this.yandeSearchTags.getValue();
	}

	public void setYandeTags(String tags) {
		this.yandeSearchTags.setValue(tags != null ? tags : "");
	}

	public String getKonachanTags() {
		return this.konachanSearchTags.getValue();
	}

	public void setKonachanTags(String tags) {
		this.konachanSearchTags.setValue(tags != null ? tags : "");
	}

	public String getAibooruTags() {
		return this.aibooruSearchTags.getValue();
	}

	public void setAibooruTags(String tags) {
		this.aibooruSearchTags.setValue(tags != null ? tags : "");
	}

	public String getE621Tags() {
		return this.e621SearchTags.getValue();
	}

	public void setE621Tags(String tags) {
		this.e621SearchTags.setValue(tags != null ? tags : "");
	}

	public String getWaifuTag() {
		return this.waifuCustomTag.getValue();
	}

	public void setWaifuTag(String tag) {
		this.waifuCustomTag.setValue(tag != null ? tag : "");
	}

	public void setPurrbotNsfwTag(PurrBotNsfwTag tag) {
		this.purrbotNsfwTag.setValue(tag);
	}

	public void setCyclePurrbot(boolean cycle) {
		this.cyclePurrbot.setValue(cycle);
	}

	public String getWebhookUrl() {
		return this.webhookUrl.getValue();
	}

	public void setWebhookUrl(String url) {
		this.webhookUrl.setValue(url != null ? url : "");
	}

	public boolean isWebhookEnabled() {
		return this.enableWebhook.getValue();
	}

	public void setWebhookEnabled(boolean enabled) {
		this.enableWebhook.setValue(enabled);
	}

	public void setStrictNSFW(boolean strict) {
		this.strictNSFW.setValue(strict);
	}

	private void loadImage() {
		if (!this.locked.compareAndSet(false, true)) {
			return;
		}

		this.loaderExecutor.submit(() -> {
			try {
				ImageMetadata meta = this.fetchImageMetadata();
				if (meta == null || meta.url == null) {
					return;
				}

				String urlStr = meta.url;
				byte[] rawBytes;
				if (urlStr.startsWith("local://")) {
					if (this.localImageFiles.isEmpty()) {
						return;
					}
					File file = this.localImageFiles.get(this.localImageIndex);
					this.localImageIndex = (this.localImageIndex + 1) % this.localImageFiles.size();
					meta.author = file.getName();
					rawBytes = Files.readAllBytes(file.toPath());
				} else {
					rawBytes = downloadBytes(urlStr, this.source.getValue() == Source.E621 ? E621_USER_AGENT : USER_AGENT);
					if (rawBytes != null && rawBytes.length > 0 && this.autoDownload.getValue()) {
						saveImageLocally(rawBytes, urlStr);
					}
				}

				if (rawBytes == null || rawBytes.length == 0) {
					return;
				}

				// Enviar para Discord Webhook se habilitado
				if (this.enableWebhook.getValue() && !this.webhookUrl.getValue().trim().isEmpty()) {
					DiscordWebhookSender.send(this.webhookUrl.getValue().trim(), meta);
				}

				if (GifDecoder.isGif(rawBytes)) {
					List<RawFrame> decoded = GifDecoder.decode(rawBytes, this.maxGifFrames.getValue());
					if (!decoded.isEmpty()) {
						this.pendingUpload = decoded;
					}
				} else {
					BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawBytes));
					if (img != null) {
						// Downscale if ridiculously large (e.g. 4K/8K uncompressed image) to preserve VRAM & framerate
						BufferedImage optimized = downscaleImage(img, 1280);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(optimized, "png", baos);

						List<RawFrame> list = new ArrayList<>();
						list.add(new RawFrame(baos.toByteArray(), optimized.getWidth(), optimized.getHeight(), 100));
						this.pendingUpload = list;
					}
				}
			} catch (Exception ignored) {
			} finally {
				this.locked.set(false);
			}
		});
	}

	private static BufferedImage downscaleImage(BufferedImage src, int maxDim) {
		int w = src.getWidth();
		int h = src.getHeight();
		if (w <= maxDim && h <= maxDim) {
			return src;
		}
		double scale = Math.min((double) maxDim / w, (double) maxDim / h);
		int targetW = Math.max(1, (int) (w * scale));
		int targetH = Math.max(1, (int) (h * scale));

		BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = scaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g2.drawImage(src, 0, 0, targetW, targetH, null);
		g2.dispose();
		return scaled;
	}

	private String buildBooruTagQuery(String userTags, String ratingParam) {
		List<String> parts = new ArrayList<>();
		if (ratingParam != null && !ratingParam.isEmpty()) {
			parts.add(ratingParam);
		}
		if (userTags != null) {
			String trimmed = userTags.trim();
			if (!trimmed.isEmpty()) {
				String[] tokens = trimmed.split("[,\\s]+");
				for (String token : tokens) {
					if (!token.isEmpty()) {
						parts.add(URLEncoder.encode(token, StandardCharsets.UTF_8));
					}
				}
			}
		}

		if (this.strictNSFW.getValue() && (userTags == null || userTags.trim().isEmpty())) {
			String spicyTag = DEFAULT_NSFW_BOORU_TAGS.get(this.random.nextInt(DEFAULT_NSFW_BOORU_TAGS.size()));
			parts.add(URLEncoder.encode(spicyTag, StandardCharsets.UTF_8));
		}

		return String.join("+", parts);
	}

	private ImageMetadata fetchImageMetadata() {
		try {
			switch (this.source.getValue()) {
				case YandeRE -> {
					String tagQuery = buildBooruTagQuery(this.yandeSearchTags.getValue(), this.yandeRating.getValue().param);
					int page = this.yandeRandomPage.getValue() ? (this.random.nextInt(35) + 1) : 1;

					JsonObject chosen = fetchMoebooruPost("https://yande.re/post.json", tagQuery, page, this.strictNSFW.getValue());
					if (chosen == null && page != 1) {
						chosen = fetchMoebooruPost("https://yande.re/post.json", tagQuery, 1, this.strictNSFW.getValue());
					}

					if (chosen != null) {
						String imgUrl = getMoebooruImageUrl(chosen);
						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "Yande.re");
							populateMoebooruMetadata(meta, chosen, "https://yande.re/post/show/");
							return meta;
						}
					}
					return null;
				}

				case Konachan -> {
					String tagQuery = buildBooruTagQuery(this.konachanSearchTags.getValue(), this.konachanRating.getValue().param);
					int page = this.konachanRandomPage.getValue() ? (this.random.nextInt(35) + 1) : 1;

					JsonObject chosen = fetchMoebooruPost("https://konachan.com/post.json", tagQuery, page, this.strictNSFW.getValue());
					if (chosen == null && page != 1) {
						chosen = fetchMoebooruPost("https://konachan.com/post.json", tagQuery, 1, this.strictNSFW.getValue());
					}

					if (chosen != null) {
						String imgUrl = getMoebooruImageUrl(chosen);
						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "Konachan");
							populateMoebooruMetadata(meta, chosen, "https://konachan.com/post/show/");
							return meta;
						}
					}
					return null;
				}

				case AIBooru -> {
					String tagQuery = buildBooruTagQuery(this.aibooruSearchTags.getValue(), this.aibooruRating.getValue().param);
					int page = this.aibooruRandomPage.getValue() ? (this.random.nextInt(25) + 1) : 1;

					JsonObject chosen = fetchDanbooruPost("https://aibooru.online/posts.json", tagQuery, page, this.strictNSFW.getValue(), USER_AGENT);
					if (chosen == null && page != 1) {
						chosen = fetchDanbooruPost("https://aibooru.online/posts.json", tagQuery, 1, this.strictNSFW.getValue(), USER_AGENT);
					}

					if (chosen != null) {
						String imgUrl = getDanbooruImageUrl(chosen);
						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "AIBooru");
							if (chosen.has("id")) {
								meta.postUrl = "https://aibooru.online/posts/" + chosen.get("id").getAsString();
							}
							if (chosen.has("tag_string_artist")) {
								meta.author = chosen.get("tag_string_artist").getAsString();
							}
							if (chosen.has("source") && !chosen.get("source").isJsonNull()) {
								meta.sourceOrigin = chosen.get("source").getAsString();
							}
							if (chosen.has("rating")) {
								meta.rating = parseRating(chosen.get("rating").getAsString());
							}
							if (chosen.has("image_width")) {
								meta.width = chosen.get("image_width").getAsInt();
							}
							if (chosen.has("image_height")) {
								meta.height = chosen.get("image_height").getAsInt();
							}
							if (chosen.has("tag_string")) {
								meta.tags = Arrays.asList(chosen.get("tag_string").getAsString().split("\\s+"));
							}
							return meta;
						}
					}
					return null;
				}

				case E621 -> {
					String tagQuery = buildBooruTagQuery(this.e621SearchTags.getValue(), this.e621Rating.getValue().param);
					int page = this.e621RandomPage.getValue() ? (this.random.nextInt(20) + 1) : 1;

					JsonObject chosen = fetchE621Post("https://e621.net/posts.json", tagQuery, page, this.strictNSFW.getValue());
					if (chosen == null && page != 1) {
						chosen = fetchE621Post("https://e621.net/posts.json", tagQuery, 1, this.strictNSFW.getValue());
					}

					if (chosen != null) {
						String imgUrl = null;
						if (chosen.has("sample") && chosen.getAsJsonObject("sample").has("url") && !chosen.getAsJsonObject("sample").get("url").isJsonNull()) {
							imgUrl = chosen.getAsJsonObject("sample").get("url").getAsString();
						} else if (chosen.has("file") && chosen.getAsJsonObject("file").has("url") && !chosen.getAsJsonObject("file").get("url").isJsonNull()) {
							imgUrl = chosen.getAsJsonObject("file").get("url").getAsString();
						}

						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "E621");
							if (chosen.has("id")) {
								meta.postUrl = "https://e621.net/posts/" + chosen.get("id").getAsString();
							}
							if (chosen.has("rating")) {
								meta.rating = parseRating(chosen.get("rating").getAsString());
							}
							if (chosen.has("tags") && chosen.getAsJsonObject("tags").has("artist")) {
								JsonArray artists = chosen.getAsJsonObject("tags").getAsJsonArray("artist");
								if (!artists.isEmpty()) {
									meta.author = artists.get(0).getAsString();
								}
							}
							if (chosen.has("tags") && chosen.getAsJsonObject("tags").has("general")) {
								JsonArray genTags = chosen.getAsJsonObject("tags").getAsJsonArray("general");
								List<String> tList = new ArrayList<>();
								for (JsonElement t : genTags) {
									tList.add(t.getAsString());
								}
								meta.tags = tList;
							}
							return meta;
						}
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

					String url = "https://api.purrbot.site/v2/img/nsfw/" + tag + "/gif";
					HttpURLConnection conn = open(url, USER_AGENT);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonObject pRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					if (pRes.has("link") && !pRes.get("link").isJsonNull()) {
						String gifUrl = pRes.get("link").getAsString();
						ImageMetadata meta = new ImageMetadata(gifUrl, "PurrBot.site");
						meta.rating = "Explicit / Hentai GIF";
						meta.tags = List.of(tag, "nsfw_gif", "purrbot");
						return meta;
					}
					return null;
				}

				case WaifuIM -> {
					String custom = this.waifuCustomTag.getValue().trim();
					String tag;
					if (!custom.isEmpty()) {
						tag = custom.replace(' ', '-');
					} else if (this.cycleWaifu.getValue()) {
						tag = WAIFU_NSFW_CYCLE_LIST.get(this.waifuCycleIndex);
						this.waifuCycleIndex = (this.waifuCycleIndex + 1) % WAIFU_NSFW_CYCLE_LIST.size();
					} else {
						tag = this.waifuTag.getValue().name();
					}

					String wUrl = "https://api.waifu.im/images?included_tags=" + URLEncoder.encode(tag, StandardCharsets.UTF_8) + "&is_nsfw=true";
					HttpURLConnection conn = open(wUrl, USER_AGENT);
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
						String imgUrl = item.get("url").getAsString();
						ImageMetadata meta = new ImageMetadata(imgUrl, "Waifu.im");
						meta.rating = "NSFW / Hentai";
						if (item.has("source") && !item.get("source").isJsonNull()) {
							meta.sourceOrigin = item.get("source").getAsString();
						}
						if (item.has("artist") && !item.get("artist").isJsonNull()) {
							JsonObject artistObj = item.getAsJsonObject("artist");
							if (artistObj.has("name")) {
								meta.author = artistObj.get("name").getAsString();
							}
						}
						if (item.has("width")) {
							meta.width = item.get("width").getAsInt();
						}
						if (item.has("height")) {
							meta.height = item.get("height").getAsInt();
						}
						if (item.has("tags")) {
							JsonArray tArr = item.getAsJsonArray("tags");
							List<String> tList = new ArrayList<>();
							for (JsonElement tEl : tArr) {
								if (tEl.isJsonObject() && tEl.getAsJsonObject().has("name")) {
									tList.add(tEl.getAsJsonObject().get("name").getAsString());
								}
							}
							meta.tags = tList;
						}
						return meta;
					}
					return null;
				}

				case NekosLife -> {
					String url = "https://nekos.life/api/v2/img/lewd";
					HttpURLConnection conn = open(url, USER_AGENT);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonObject nRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					if (nRes.has("url") && !nRes.get("url").isJsonNull()) {
						String imgUrl = nRes.get("url").getAsString();
						ImageMetadata meta = new ImageMetadata(imgUrl, "Nekos.life");
						meta.rating = "Explicit / Lewd";
						meta.tags = List.of("lewd", "neko", "ecchi");
						return meta;
					}
					return null;
				}

				case LocalFolder -> {
					ImageMetadata meta = new ImageMetadata("local://image", "Local Folder");
					meta.rating = "Local";
					return meta;
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static String getMoebooruImageUrl(JsonObject chosen) {
		if (chosen.has("sample_url") && !chosen.get("sample_url").isJsonNull()) {
			return chosen.get("sample_url").getAsString();
		} else if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
			return chosen.get("file_url").getAsString();
		}
		return null;
	}

	private static void populateMoebooruMetadata(ImageMetadata meta, JsonObject chosen, String postBaseUrl) {
		if (chosen.has("id")) {
			meta.postUrl = postBaseUrl + chosen.get("id").getAsString();
		}
		if (chosen.has("author")) {
			meta.author = chosen.get("author").getAsString();
		}
		if (chosen.has("source") && !chosen.get("source").isJsonNull()) {
			meta.sourceOrigin = chosen.get("source").getAsString();
		}
		if (chosen.has("rating")) {
			meta.rating = parseRating(chosen.get("rating").getAsString());
		}
		if (chosen.has("width")) {
			meta.width = chosen.get("width").getAsInt();
		}
		if (chosen.has("height")) {
			meta.height = chosen.get("height").getAsInt();
		}
		if (chosen.has("tags")) {
			meta.tags = Arrays.asList(chosen.get("tags").getAsString().split("\\s+"));
		}
	}

	private static String getDanbooruImageUrl(JsonObject chosen) {
		if (chosen.has("large_file_url") && !chosen.get("large_file_url").isJsonNull()) {
			return chosen.get("large_file_url").getAsString();
		} else if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
			return chosen.get("file_url").getAsString();
		}
		return null;
	}

	private static String parseRating(String r) {
		if (r == null) return "Explicit";
		if (r.equalsIgnoreCase("e") || r.equalsIgnoreCase("explicit")) return "Explicit (NSFW)";
		if (r.equalsIgnoreCase("q") || r.equalsIgnoreCase("questionable")) return "Questionable (Ecchi)";
		if (r.equalsIgnoreCase("s") || r.equalsIgnoreCase("safe")) return "Safe";
		return r;
	}

	private JsonObject fetchMoebooruPost(String baseUrl, String tagQuery, int page, boolean filterStrictNsfw) {
		try {
			String url = baseUrl + "?limit=50&page=" + page;
			if (!tagQuery.isEmpty()) {
				url += "&tags=" + tagQuery;
			}

			HttpURLConnection conn = open(url, USER_AGENT);
			if (conn.getResponseCode() != 200) {
				return null;
			}

			JsonElement root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			if (!root.isJsonArray()) {
				return null;
			}
			JsonArray posts = root.getAsJsonArray();
			if (posts.isEmpty()) {
				return null;
			}

			List<JsonObject> candidates = new ArrayList<>();
			for (JsonElement el : posts) {
				if (!el.isJsonObject()) continue;
				JsonObject p = el.getAsJsonObject();
				if (filterStrictNsfw) {
					if (p.has("rating") && p.get("rating").getAsString().equalsIgnoreCase("s")) {
						continue;
					}
				}
				candidates.add(p);
			}

			if (candidates.isEmpty()) {
				return posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
			}

			return candidates.get(this.random.nextInt(candidates.size()));
		} catch (Exception e) {
			return null;
		}
	}

	private JsonObject fetchDanbooruPost(String baseUrl, String tagQuery, int page, boolean filterStrictNsfw, String ua) {
		try {
			String url = baseUrl + "?limit=50&page=" + page;
			if (!tagQuery.isEmpty()) {
				url += "&tags=" + tagQuery;
			}

			HttpURLConnection conn = open(url, ua);
			if (conn.getResponseCode() != 200) {
				return null;
			}

			JsonElement root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			if (!root.isJsonArray()) {
				return null;
			}
			JsonArray posts = root.getAsJsonArray();
			if (posts.isEmpty()) {
				return null;
			}

			List<JsonObject> candidates = new ArrayList<>();
			for (JsonElement el : posts) {
				if (!el.isJsonObject()) continue;
				JsonObject p = el.getAsJsonObject();
				if (filterStrictNsfw) {
					if (p.has("rating") && (p.get("rating").getAsString().equalsIgnoreCase("s") || p.get("rating").getAsString().equalsIgnoreCase("g"))) {
						continue;
					}
				}
				candidates.add(p);
			}

			if (candidates.isEmpty()) {
				return posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
			}

			return candidates.get(this.random.nextInt(candidates.size()));
		} catch (Exception e) {
			return null;
		}
	}

	private JsonObject fetchE621Post(String baseUrl, String tagQuery, int page, boolean filterStrictNsfw) {
		try {
			String url = baseUrl + "?limit=40&page=" + page;
			if (!tagQuery.isEmpty()) {
				url += "&tags=" + tagQuery;
			}

			HttpURLConnection conn = open(url, E621_USER_AGENT);
			if (conn.getResponseCode() != 200) {
				return null;
			}

			JsonObject root = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
			if (!root.has("posts")) {
				return null;
			}
			JsonArray posts = root.getAsJsonArray("posts");
			if (posts == null || posts.isEmpty()) {
				return null;
			}

			List<JsonObject> candidates = new ArrayList<>();
			for (JsonElement el : posts) {
				if (!el.isJsonObject()) continue;
				JsonObject p = el.getAsJsonObject();
				if (filterStrictNsfw) {
					if (p.has("rating") && (p.get("rating").getAsString().equalsIgnoreCase("s") || p.get("rating").getAsString().equalsIgnoreCase("g"))) {
						continue;
					}
				}
				candidates.add(p);
			}

			if (candidates.isEmpty()) {
				return posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
			}

			return candidates.get(this.random.nextInt(candidates.size()));
		} catch (Exception e) {
			return null;
		}
	}

	private static HttpURLConnection open(String urlString, String userAgent) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
		conn.setRequestProperty("User-Agent", userAgent);
		conn.setRequestProperty("Accept", "application/json, image/*, */*");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(15000);
		return conn;
	}

	private static byte[] downloadBytes(String urlStr, String userAgent) throws Exception {
		HttpURLConnection conn = open(urlStr, userAgent);
		try (InputStream in = conn.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			byte[] data = new byte[16384];
			int nRead;
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			return buffer.toByteArray();
		}
	}

	public enum Source {
		YandeRE,
		Konachan,
		AIBooru,
		PurrBot,
		WaifuIM,
		E621,
		NekosLife,
		LocalFolder
	}

	public enum AspectMode {
		Fit,
		Stretch
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
		final byte[] pngBytes;
		final int width;
		final int height;
		final int delayMs;

		RawFrame(byte[] pngBytes, int width, int height, int delayMs) {
			this.pngBytes = pngBytes;
			this.width = width;
			this.height = height;
			this.delayMs = delayMs;
		}
	}

	private static class TextureFrame {
		final TextureGraphic graphic;
		final int width;
		final int height;
		final int delay;

		TextureFrame(TextureGraphic graphic, int width, int height, int delay) {
			this.graphic = graphic;
			this.width = width;
			this.height = height;
			this.delay = delay;
		}
	}
}
