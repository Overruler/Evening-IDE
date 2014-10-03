package javax.media.jai;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public abstract class PlanarImage implements ImageJAI, RenderedImage {
	public BufferedImage getAsBufferedImage() {
		return null;
	}
	public static PlanarImage wrapRenderedImage(BufferedImage bi) {
		return null;
	}
}
