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

	// Yande.re Settings (Pesquisa livre de tags: ex. "genshin_impact", "bikini", "thighs", etc.)
	private final StringSetting yandeSearchTags = new StringSetting("YandeSearchTags", "");
	private final EnumSetting<BooruRating> yandeRating = new EnumSetting<>("YandeRating", BooruRating.Explicit);
	private final BooleanSetting yandeRandomPage = new BooleanSetting("YandeRandomPage", true);

	// Konachan Settings (Pesquisa livre de tags)
	private final StringSetting konachanSearchTags = new StringSetting("KonachanSearchTags", "");
	private final EnumSetting<BooruRating> konachanRating = new EnumSetting<>("KonachanRating", BooruRating.Explicit);
	private final BooleanSetting konachanRandomPage = new BooleanSetting("KonachanRandomPage", true);

	// PurrBot (GIFs NSFW) Settings
	private final EnumSetting<PurrBotNsfwTag> purrbotNsfwTag = new EnumSetting<>("PurrNsfwTag", PurrBotNsfwTag.blowjob);
	private final BooleanSetting cyclePurrbot = new BooleanSetting("CyclePurrBot", true);

	// Waifu.im Settings (Pesquisa por tag customizada ou seletor enum)
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

	// GIF Settings
	private final BooleanSetting animateGifs = new BooleanSetting("AnimateGifs", true);
	private final NumberSetting<Integer> maxGifFrames = new NumberSetting<>("MaxGifFrames", 150, 2, 500);

	// Rastreamento para recarregamento instantâneo
	private Source lastSource = null;
	private String lastYandeTags = null;
	private BooruRating lastYandeRating = null;
	private String lastKonachanTags = null;
	private BooruRating lastKonachanRating = null;
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

		this.purrbotNsfwTag.setVisibility(() -> this.source.getValue() == Source.PurrBot);
		this.cyclePurrbot.setVisibility(() -> this.source.getValue() == Source.PurrBot);

		this.waifuCustomTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.waifuTag.setVisibility(() -> this.source.getValue() == Source.WaifuIM);
		this.cycleWaifu.setVisibility(() -> this.source.getValue() == Source.WaifuIM);

		this.webhookUrl.setVisibility(() -> this.enableWebhook.getValue());

		this.registerSettings(
				this.source,
				// Yande.re Tag Search & Ratings
				this.yandeSearchTags,
				this.yandeRating,
				this.yandeRandomPage,
				// Konachan Tag Search & Ratings
				this.konachanSearchTags,
				this.konachanRating,
				this.konachanRandomPage,
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

		// Detecção de mudança de configurações para recarregamento instantâneo
		boolean sourceChanged = this.lastSource != this.source.getValue();
		boolean yandeChanged = this.source.getValue() == Source.YandeRE && (!Objects.equals(this.lastYandeTags, this.yandeSearchTags.getValue()) || this.lastYandeRating != this.yandeRating.getValue());
		boolean konachanChanged = this.source.getValue() == Source.Konachan && (!Objects.equals(this.lastKonachanTags, this.konachanSearchTags.getValue()) || this.lastKonachanRating != this.konachanRating.getValue());
		boolean waifuChanged = this.source.getValue() == Source.WaifuIM && (!Objects.equals(this.lastWaifuTag, this.waifuCustomTag.getValue()) || this.lastWaifuEnumTag != this.waifuTag.getValue());
		boolean purrChanged = this.source.getValue() == Source.PurrBot && (this.lastPurrTag != this.purrbotNsfwTag.getValue());

		if (sourceChanged || yandeChanged || konachanChanged || waifuChanged || purrChanged) {
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

	private void loadImage() {
		if (!this.locked.compareAndSet(false, true)) {
			return;
		}

		new Thread(() -> {
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
					rawBytes = downloadBytes(urlStr);
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
		return String.join("+", parts);
	}

	private ImageMetadata fetchImageMetadata() {
		try {
			switch (this.source.getValue()) {
				case YandeRE -> {
					String tagQuery = buildBooruTagQuery(this.yandeSearchTags.getValue(), this.yandeRating.getValue().param);
					int page = this.yandeRandomPage.getValue() ? (this.random.nextInt(35) + 1) : 1;

					JsonObject chosen = fetchBooruPost("https://yande.re/post.json", tagQuery, page);
					if (chosen == null && page != 1) {
						chosen = fetchBooruPost("https://yande.re/post.json", tagQuery, 1);
					}

					if (chosen != null) {
						String imgUrl = null;
						if (chosen.has("sample_url") && !chosen.get("sample_url").isJsonNull()) {
							imgUrl = chosen.get("sample_url").getAsString();
						} else if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
							imgUrl = chosen.get("file_url").getAsString();
						}

						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "Yande.re");
							if (chosen.has("id")) {
								meta.postUrl = "https://yande.re/post/show/" + chosen.get("id").getAsString();
							}
							if (chosen.has("author")) {
								meta.author = chosen.get("author").getAsString();
							}
							if (chosen.has("source") && !chosen.get("source").isJsonNull()) {
								meta.sourceOrigin = chosen.get("source").getAsString();
							}
							if (chosen.has("rating")) {
								meta.rating = chosen.get("rating").getAsString();
							}
							if (chosen.has("width")) {
								meta.width = chosen.get("width").getAsInt();
							}
							if (chosen.has("height")) {
								meta.height = chosen.get("height").getAsInt();
							}
							if (chosen.has("tags")) {
								String tagsStr = chosen.get("tags").getAsString();
								meta.tags = Arrays.asList(tagsStr.split("\\s+"));
							}
							return meta;
						}
					}
					return null;
				}

				case Konachan -> {
					String tagQuery = buildBooruTagQuery(this.konachanSearchTags.getValue(), this.konachanRating.getValue().param);
					int page = this.konachanRandomPage.getValue() ? (this.random.nextInt(35) + 1) : 1;

					JsonObject chosen = fetchBooruPost("https://konachan.com/post.json", tagQuery, page);
					if (chosen == null && page != 1) {
						chosen = fetchBooruPost("https://konachan.com/post.json", tagQuery, 1);
					}

					if (chosen != null) {
						String imgUrl = null;
						if (chosen.has("sample_url") && !chosen.get("sample_url").isJsonNull()) {
							imgUrl = chosen.get("sample_url").getAsString();
						} else if (chosen.has("file_url") && !chosen.get("file_url").isJsonNull()) {
							imgUrl = chosen.get("file_url").getAsString();
						}

						if (imgUrl != null) {
							ImageMetadata meta = new ImageMetadata(imgUrl, "Konachan");
							if (chosen.has("id")) {
								meta.postUrl = "https://konachan.com/post/show/" + chosen.get("id").getAsString();
							}
							if (chosen.has("author")) {
								meta.author = chosen.get("author").getAsString();
							}
							if (chosen.has("source") && !chosen.get("source").isJsonNull()) {
								meta.sourceOrigin = chosen.get("source").getAsString();
							}
							if (chosen.has("rating")) {
								meta.rating = chosen.get("rating").getAsString();
							}
							if (chosen.has("width")) {
								meta.width = chosen.get("width").getAsInt();
							}
							if (chosen.has("height")) {
								meta.height = chosen.get("height").getAsInt();
							}
							if (chosen.has("tags")) {
								String tagsStr = chosen.get("tags").getAsString();
								meta.tags = Arrays.asList(tagsStr.split("\\s+"));
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

					String url = "https://purrbot.site/api/img/nsfw/" + tag + "/gif";
					HttpURLConnection conn = open(url);
					if (conn.getResponseCode() != 200) {
						return null;
					}
					JsonObject pRes = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
					if (pRes.has("link") && !pRes.get("link").isJsonNull()) {
						String gifUrl = pRes.get("link").getAsString();
						ImageMetadata meta = new ImageMetadata(gifUrl, "PurrBot.site");
						meta.rating = "NSFW / GIF";
						meta.tags = List.of(tag, "gif", "purrbot");
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
						String imgUrl = item.get("url").getAsString();
						ImageMetadata meta = new ImageMetadata(imgUrl, "Waifu.im");
						meta.rating = "NSFW";
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

	private JsonObject fetchBooruPost(String baseUrl, String tagQuery, int page) {
		try {
			String url = baseUrl + "?limit=30&page=" + page;
			if (!tagQuery.isEmpty()) {
				url += "&tags=" + tagQuery;
			}

			HttpURLConnection conn = open(url);
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
			return posts.get(this.random.nextInt(posts.size())).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
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
