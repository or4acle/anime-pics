package me.farmador.animepics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JoiManager {

	public enum JoiStyle {
		Gentle,
		Strict,
		Humiliation,
		Hardcore,
		Edging,
		Dynamic
	}

	public enum TempoMode {
		SLOW("Slow & Steady (40 BPM)", 40, 0xFF4A90E2),
		MEDIUM("Moderate Pace (75 BPM)", 75, 0xFF50E3C2),
		FAST("Intense Speed (120 BPM)", 120, 0xFFF5A623),
		RUSH("Full Speed Frenzy! (160 BPM)", 160, 0xFFFF416C),
		EDGE("HOLD & EDGE! (Stop Strokng)", 0, 0xFFFF007F),
		HANDSFREE("Hands Free! Look Only", 0, 0xFFBD10E0);

		public final String displayName;
		public final int bpm;
		public final int color;

		TempoMode(String displayName, int bpm, int color) {
			this.displayName = displayName;
			this.bpm = bpm;
			this.color = color;
		}
	}

	public record PerformanceScore(String rank, String title, int stars, String description, int colorHex) {}

	private final Random random = new Random();
	private final File statsFile;
	private JoiStats stats;

	// Estado da Sessão Atual
	private long sessionStartTime = 0;
	private int currentSessionEdges = 0;
	private boolean active = false;

	// JOI Texto e Ritmo
	private String currentPrompt = "Get ready and look at the screen...";
	private TempoMode currentTempo = TempoMode.SLOW;
	private long lastPromptChange = 0;
	private long promptIntervalMs = 8000;
	private long phaseChangeTime = 0;
	private long currentPhaseDurationMs = 15000;

	// Metrônomo visual
	private long lastBeatTime = 0;
	private boolean beatState = false;

	// Banco de frases categorizadas
	private static final List<String> HUMILIATION_PHRASES = Arrays.asList(
			"Look at you, stroking your useless stick to 2D pixels in Minecraft.",
			"Nem 2 minutos e você já tá suando e tremendo? Que patético kkkkk",
			"You really thought you had free will? A PNG file owns your entire existence.",
			"Olha como ela nem sabe que você existe enquanto você baba na tela kkkkk",
			"Hands off, loser! You don't deserve to touch yourself right now.",
			"Imagine if someone walked into your room and saw you jerking off inside Minecraft.",
			"Look at that desperate grip. Stroke slower, obedient little gooner.",
			"Nem pense em gozar agora! Você não tem permissão nem pra respirar alto.",
			"You're literally getting mogged by fictional drawings. Pathetic.",
			"Look at the screen and accept what you are: a total 2D addict.",
			"Mãos pra cima seu viciado, olha o estado do indivíduo kkkkk",
			"Did I say you could speed up? Obey the tempo, you weak-willed simp.",
			"Gooning inside a Minecraft cheat client... peak degeneracy achieved.",
			"She wouldn't even look in your direction in real life. Keep worshipping the screen.",
			"Every stroke proves how powerless you are against anime drawings.",
			"Olha pra você, totalmente hipnotizado por pixels coloridos kkkkkk",
			"You're shaking already? Weak. You're going to hold this edge forever.",
			"Stroke on command, simp. 1, 2, 1, 2... do what you're told."
	);

	private static final List<String> GENTLE_PHRASES = Arrays.asList(
			"Take your time, enjoy every detail of her body...",
			"Stroke nice and slow, let the pleasure build up gently.",
			"Look into her eyes, she loves having all your attention.",
			"Match your breathing to her rhythm... slow and smooth.",
			"You're doing so well, just relax and let her tease you.",
			"Feel how good this feels... don't rush, savor the moment.",
			"Keep your eyes locked on her while your hand glides smoothly.",
			"Every curve is made for your pleasure. Just enjoy.",
			"Slow down if it feels too good, make this last for her.",
			"Breathe in deep, stroke down with each exhale..."
	);

	private static final List<String> STRICT_PHRASES = Arrays.asList(
			"Do NOT look away. Keep your eyes glued to her!",
			"Only stroke at the exact tempo instructed. No rushing!",
			"You are not allowed to finish yet. Control yourself.",
			"Focus on her moans and details. You obey the screen.",
			"Did I say you could go fast? Maintain the pace!",
			"Feel the ache building up. Hold it right there.",
			"Look at what you're worshiping. Don't dare lose focus.",
			"Every stroke belongs to her. You edge when told to edge.",
			"Control that urge. The longer you hold, the better it gets.",
			"Hands in sync with the beat. 1, 2, 1, 2... perfect obedience."
	);

	private static final List<String> HARDCORE_PHRASES = Arrays.asList(
			"Pump faster! Look at how dirty she's looking at you!",
			"Stroke harder with every beat, fill your head with her!",
			"Can you feel how close you're getting? Don't hold back now!",
			"Look at those curves, pump to the maximum rhythm!",
			"Lose your mind to this artwork, let the ecstasy take over!",
			"Every inch of this art is pure filth. Stroke like you need it!",
			"Faster, harder! Don't let your grip slip for a second!",
			"Overload your senses, let all your discipline melt away!",
			"Stroke as fast as you can, look straight at her climax!"
	);

	private static final List<String> EDGING_PHRASES = Arrays.asList(
			"⚠️ EDGE IMMINENT: Bring yourself right to the brink, then FREEZE!",
			"🛑 HANDS OFF! Breathe slowly and let the sensation peak without spilling.",
			"Hold that edge... feel the heat pulsing through your body.",
			"Now resume gently... keep yourself hovering right on the edge.",
			"Another edge conquered! You're building an incredible buildup.",
			"Bring it right to 99%... do NOT cross the line!",
			"Breathe... 5, 4, 3, 2, 1... resume slow strokes now.",
			"Feel how sensitive you are right now? That's pure control.",
			"Stack another edge onto your score. The release will be explosive."
	);

	public JoiManager(File gameDir) {
		File dataDir = new File(gameDir, "rusherhack" + File.separator + "animepics");
		this.statsFile = new File(dataDir, "joi_stats.json");
		this.stats = JoiStats.load(this.statsFile);
	}

	public void startSession() {
		this.sessionStartTime = System.currentTimeMillis();
		this.currentSessionEdges = 0;
		this.active = true;
		this.currentTempo = TempoMode.SLOW;
		this.currentPrompt = "JOI Session started! Relax, focus on the art, and follow the rhythm.";
		this.lastPromptChange = System.currentTimeMillis();
		this.phaseChangeTime = System.currentTimeMillis();
		this.currentPhaseDurationMs = 12000;
	}

	public void stopSession() {
		this.active = false;
	}

	public boolean isActive() {
		return this.active;
	}

	public void update(JoiStyle style) {
		long now = System.currentTimeMillis();

		if (!this.active || this.sessionStartTime == 0) {
			this.startSession();
		}

		// Atualização de Metrônomo
		if (this.currentTempo.bpm > 0) {
			long beatInterval = 60000L / this.currentTempo.bpm;
			if (now - this.lastBeatTime >= beatInterval) {
				this.lastBeatTime = now;
				this.beatState = !this.beatState;
			}
		}

		// Troca de Fase / Tempo
		if (now - this.phaseChangeTime >= this.currentPhaseDurationMs) {
			this.phaseChangeTime = now;
			this.transitionPhase(style);
		}

		// Troca de Frase de Incentivo
		if (now - this.lastPromptChange >= this.promptIntervalMs) {
			this.lastPromptChange = now;
			this.currentPrompt = pickRandomPhrase(style, this.currentTempo);
		}
	}

	private void transitionPhase(JoiStyle style) {
		long elapsedSec = getSessionElapsedSeconds();

		if (style == JoiStyle.Edging) {
			if (this.currentTempo != TempoMode.EDGE && this.currentTempo != TempoMode.HANDSFREE) {
				if (this.random.nextBoolean()) {
					this.currentTempo = TempoMode.EDGE;
					this.currentPhaseDurationMs = 8000 + this.random.nextInt(7000);
					this.currentPrompt = "⚠️ EDGE TIME! Bring yourself right to the brink and STOP touching!";
					return;
				}
			}
		}

		// Progressão natural com base no tempo de sessão
		if (elapsedSec < 90) {
			// Aquecimento
			this.currentTempo = (this.random.nextInt(3) == 0) ? TempoMode.HANDSFREE : TempoMode.SLOW;
			this.currentPhaseDurationMs = 10000 + this.random.nextInt(8000);
		} else if (elapsedSec < 300) {
			// Fase Moderada
			TempoMode[] choices = {TempoMode.SLOW, TempoMode.MEDIUM, TempoMode.MEDIUM};
			this.currentTempo = choices[this.random.nextInt(choices.length)];
			this.currentPhaseDurationMs = 12000 + this.random.nextInt(10000);
		} else if (elapsedSec < 600) {
			// Fase Intensa
			TempoMode[] choices = {TempoMode.MEDIUM, TempoMode.FAST, TempoMode.EDGE};
			this.currentTempo = choices[this.random.nextInt(choices.length)];
			this.currentPhaseDurationMs = 10000 + this.random.nextInt(10000);
		} else {
			// Climax / Hardcore
			TempoMode[] choices = {TempoMode.FAST, TempoMode.RUSH, TempoMode.EDGE, TempoMode.MEDIUM};
			this.currentTempo = choices[this.random.nextInt(choices.length)];
			this.currentPhaseDurationMs = 8000 + this.random.nextInt(8000);
		}
	}

	private String pickRandomPhrase(JoiStyle style, TempoMode tempo) {
		if (tempo == TempoMode.EDGE || tempo == TempoMode.HANDSFREE) {
			return EDGING_PHRASES.get(this.random.nextInt(EDGING_PHRASES.size()));
		}

		List<String> pool;
		switch (style) {
			case Gentle -> pool = GENTLE_PHRASES;
			case Strict -> pool = STRICT_PHRASES;
			case Humiliation -> pool = HUMILIATION_PHRASES;
			case Hardcore -> pool = HARDCORE_PHRASES;
			case Edging -> pool = EDGING_PHRASES;
			case Dynamic -> {
				int r = this.random.nextInt(100);
				if (r < 20) pool = GENTLE_PHRASES;
				else if (r < 45) pool = HUMILIATION_PHRASES;
				else if (r < 70) pool = STRICT_PHRASES;
				else if (r < 88) pool = HARDCORE_PHRASES;
				else pool = EDGING_PHRASES;
			}
			default -> pool = HUMILIATION_PHRASES;
		}

		return pool.get(this.random.nextInt(pool.size()));
	}

	public void recordEdge() {
		this.currentSessionEdges++;
		this.stats.totalEdges++;
		this.stats.save(this.statsFile);
		this.currentTempo = TempoMode.EDGE;
		this.phaseChangeTime = System.currentTimeMillis();
		this.currentPhaseDurationMs = 10000;
		this.currentPrompt = "🧗 EDGE #" + this.currentSessionEdges + " LOGGED! Hands off and breathe deeply for 10s...";
	}

	public NutResult recordNut(ImageMetadata currentImage, String userNote) {
		long now = System.currentTimeMillis();
		long durationSec = getSessionElapsedSeconds();
		if (durationSec <= 0) {
			durationSec = 1;
		}

		this.stats.totalNuts++;
		this.stats.totalSessionTimeSeconds += durationSec;

		if (this.stats.fastestNutSeconds == 0 || durationSec < this.stats.fastestNutSeconds) {
			this.stats.fastestNutSeconds = durationSec;
		}
		if (durationSec > this.stats.longestNutSeconds) {
			this.stats.longestNutSeconds = durationSec;
		}

		PerformanceScore score = calculateScore(durationSec, this.currentSessionEdges);

		String imgSource = currentImage != null ? currentImage.sourceSite : "Unknown";
		String imgUrl = currentImage != null ? currentImage.url : "";
		String postUrl = currentImage != null ? currentImage.postUrl : "";

		JoiStats.NutRecord record = new JoiStats.NutRecord(
				now,
				durationSec,
				this.currentSessionEdges,
				score.rank,
				score.title,
				imgSource,
				imgUrl,
				postUrl,
				userNote != null ? userNote.trim() : ""
		);

		this.stats.history.add(record);
		this.stats.lastNutTimestamp = now;
		this.stats.save(this.statsFile);

		int loggedEdges = this.currentSessionEdges;

		// Reinicia a sessão para o próximo ciclo
		this.startSession();
		this.currentPrompt = "💦 Climax recorded (" + score.rank + " - " + score.title + ")! Take a deep breath to recover.";

		return new NutResult(record, score, this.stats.totalNuts, loggedEdges, durationSec);
	}

	public static PerformanceScore calculateScore(long durationSec, int edges) {
		int basePoints = (int) (durationSec / 10);
		int edgeBonus = edges * 50;
		int totalScore = basePoints + edgeBonus;

		if (durationSec < 120) {
			return new PerformanceScore("C", "⚡ Instant Nut Loser", 1, "Under 2 minutes?! Total lack of discipline against a 2D PNG! Pathetic!", 0xFFFFA07A);
		} else if (durationSec < 360) {
			if (edges >= 2) {
				return new PerformanceScore("A", "🔥 Controlled Spark", 3, "Short but resisted a few edges. At least you tried!", 0xFFFF7F50);
			}
			return new PerformanceScore("B", "🐔 2-Minute Noodle Simp", 2, "Barely held on. Completely weak against fictional anime drawings.", 0xFFFFD700);
		} else if (durationSec < 900) {
			if (edges >= 3) {
				return new PerformanceScore("S", "💎 Edge Connoisseur", 4, "High stamina and multiple edge stacks! A disciplined degenerate.", 0xFF00FF7F);
			}
			return new PerformanceScore("A", "🎯 Certified 2D Degenerate", 3, "Decent endurance, but your brain is officially addicted to pixels.", 0xFF32CD32);
		} else if (durationSec < 1800) {
			if (edges >= 4) {
				return new PerformanceScore("SS", "👑 God of Degeneracy", 5, "Over half an hour stroking to a block game client. Peak stamina, zero grass touched.", 0xFF00E5FF);
			}
			return new PerformanceScore("S", "💎 Iron Will Gooner", 4, "Strong resistance against constant 2D temptation. Impressive focus.", 0xFF1E90FF);
		} else {
			return new PerformanceScore("SSS", "🌌 Ascended No-Lifer Transcendence", 5, "Stamina over 45 min! Go touch grass immediately, you're transcending reality.", 0xFFFF00FF);
		}
	}

	public void resetStats() {
		this.stats = new JoiStats();
		this.stats.save(this.statsFile);
		this.startSession();
	}

	public long getSessionElapsedSeconds() {
		if (this.sessionStartTime == 0) return 0;
		return (System.currentTimeMillis() - this.sessionStartTime) / 1000L;
	}

	public String formatDuration(long seconds) {
		long m = seconds / 60;
		long s = seconds % 60;
		if (m >= 60) {
			long h = m / 60;
			m = m % 60;
			return String.format("%dh %02dm %02ds", h, m, s);
		}
		return String.format("%02dm %02ds", m, s);
	}

	public String getCurrentPrompt() {
		return this.currentPrompt;
	}

	public TempoMode getCurrentTempo() {
		return this.currentTempo;
	}

	public boolean getBeatState() {
		return this.beatState;
	}

	public int getCurrentSessionEdges() {
		return this.currentSessionEdges;
	}

	public JoiStats getStats() {
		return this.stats;
	}

	public record NutResult(
			JoiStats.NutRecord record,
			PerformanceScore score,
			int allTimeNuts,
			int sessionEdges,
			long durationSeconds
	) {}
}
