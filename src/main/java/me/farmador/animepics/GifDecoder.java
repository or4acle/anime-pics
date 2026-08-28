package me.farmador.animepics;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class GifDecoder {

	private GifDecoder() {
	}

	static boolean isGif(byte[] bytes) {
		return bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
	}

	static List<AnimePicsModule.RawFrame> decode(byte[] gifBytes, int maxFrames) throws Exception {
		List<AnimePicsModule.RawFrame> frames = new ArrayList<>();
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
		if (!readers.hasNext()) {
			return frames;
		}
		ImageReader reader = readers.next();

		try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(gifBytes))) {
			reader.setInput(iis, false);
			int frameCount = reader.getNumImages(true);
			int limit = Math.min(frameCount, maxFrames);

			int screenW = -1;
			int screenH = -1;
			IIOMetadata streamMetadata = reader.getStreamMetadata();
			if (streamMetadata != null) {
				IIOMetadataNode streamRoot = (IIOMetadataNode) streamMetadata.getAsTree("javax_imageio_gif_stream_1.0");
				IIOMetadataNode lsd = getChildNode(streamRoot, "LogicalScreenDescriptor");
				if (lsd != null) {
					screenW = parseIntSafe(lsd.getAttribute("logicalScreenWidth"), -1);
					screenH = parseIntSafe(lsd.getAttribute("logicalScreenHeight"), -1);
				}
			}

			BufferedImage canvas = null;
			BufferedImage restoreSnapshot = null;

			for (int i = 0; i < limit; i++) {
				BufferedImage frame = reader.read(i);
				IIOMetadata metadata = reader.getImageMetadata(i);
				IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");

				int delayCs = 10;
				String disposal = "none";
				int fx = 0;
				int fy = 0;

				IIOMetadataNode gce = getChildNode(root, "GraphicControlExtension");
				if (gce != null) {
					delayCs = parseIntSafe(gce.getAttribute("delayTime"), delayCs);
					String disp = gce.getAttribute("disposalMethod");
					if (disp != null && !disp.isEmpty()) {
						disposal = disp;
					}
				}
				IIOMetadataNode descriptor = getChildNode(root, "ImageDescriptor");
				if (descriptor != null) {
					fx = parseIntSafe(descriptor.getAttribute("imageLeftPosition"), 0);
					fy = parseIntSafe(descriptor.getAttribute("imageTopPosition"), 0);
				}

				if (canvas == null) {
					int w = screenW > 0 ? screenW : Math.max(frame.getWidth(), fx + frame.getWidth());
					int h = screenH > 0 ? screenH : Math.max(frame.getHeight(), fy + frame.getHeight());
					canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				}

				if ("restoreToPrevious".equals(disposal)) {
					restoreSnapshot = copyImage(canvas);
				}

				Graphics2D g = canvas.createGraphics();
				g.drawImage(frame, fx, fy, null);
				g.dispose();

				frames.add(new AnimePicsModule.RawFrame(copyImage(canvas), Math.max(delayCs * 10, 20)));

				if ("restoreToBackgroundColor".equals(disposal)) {
					Graphics2D clear = canvas.createGraphics();
					clear.setComposite(AlphaComposite.Clear);
					clear.fillRect(fx, fy, frame.getWidth(), frame.getHeight());
					clear.dispose();
				} else if ("restoreToPrevious".equals(disposal) && restoreSnapshot != null) {
					canvas = restoreSnapshot;
				}
			}
		} finally {
			reader.dispose();
		}
		return frames;
	}

	private static IIOMetadataNode getChildNode(IIOMetadataNode root, String name) {
		if (root == null) {
			return null;
		}
		for (int i = 0; i < root.getLength(); i++) {
			if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
				return (IIOMetadataNode) root.item(i);
			}
		}
		return null;
	}

	private static int parseIntSafe(String s, int fallback) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	private static BufferedImage copyImage(BufferedImage src) {
		BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copy.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return copy;
	}
}
