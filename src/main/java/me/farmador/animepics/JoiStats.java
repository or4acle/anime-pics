package me.farmador.animepics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JoiStats {

	public int totalNuts = 0;
	public int totalEdges = 0;
	public long totalSessionTimeSeconds = 0;
	public long fastestNutSeconds = 0;
	public long longestNutSeconds = 0;
	public long lastNutTimestamp = 0;
	public List<NutRecord> history = new ArrayList<>();

	public static class NutRecord {
		public long timestamp;
		public long durationSeconds;
		public int edges;
		public String scoreRank;
		public String scoreTitle;
		public String imageSource;
		public String imageUrl;
		public String postUrl;
		public String note;

		public NutRecord() {
		}

		public NutRecord(long timestamp, long durationSeconds, int edges, String scoreRank, String scoreTitle, String imageSource, String imageUrl, String postUrl, String note) {
			this.timestamp = timestamp;
			this.durationSeconds = durationSeconds;
			this.edges = edges;
			this.scoreRank = scoreRank;
			this.scoreTitle = scoreTitle;
			this.imageSource = imageSource;
			this.imageUrl = imageUrl;
			this.postUrl = postUrl;
			this.note = note;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static JoiStats load(File file) {
		if (file.exists()) {
			try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
				JoiStats stats = GSON.fromJson(reader, JoiStats.class);
				if (stats != null) {
					if (stats.history == null) {
						stats.history = new ArrayList<>();
					}
					return stats;
				}
			} catch (Exception e) {
				System.err.println("[AnimePics] Could not read joi_stats.json: " + e.getMessage());
			}
		}
		return new JoiStats();
	}

	public void save(File file) {
		try {
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			System.err.println("[AnimePics] Could not save joi_stats.json: " + e.getMessage());
		}
	}
}
