package me.farmador.animepics;

import java.util.List;

public class ImageMetadata {
	public String url;
	public String sourceSite;
	public String postUrl;
	public String author;
	public String sourceOrigin;
	public List<String> tags;
	public String rating;
	public int width;
	public int height;

	public ImageMetadata(String url, String sourceSite) {
		this.url = url;
		this.sourceSite = sourceSite;
	}
}
