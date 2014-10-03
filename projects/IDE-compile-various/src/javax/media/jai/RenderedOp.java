package javax.media.jai;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Vector;

public class RenderedOp extends PlanarImage implements OperationNode, PropertyChangeListener, Serializable {
	@Override
	public void propertyChange(PropertyChangeEvent evt) {}
	@Override
	public Vector<RenderedImage> getSources() {
		return null;
	}
	@Override
	public Object getProperty(String name) {
		return null;
	}
	@Override
	public String[] getPropertyNames() {
		return null;
	}
	@Override
	public ColorModel getColorModel() {
		return null;
	}
	@Override
	public SampleModel getSampleModel() {
		return null;
	}
	@Override
	public int getWidth() {
		return 0;
	}
	@Override
	public int getHeight() {
		return 0;
	}
	@Override
	public int getMinX() {
		return 0;
	}
	@Override
	public int getMinY() {
		return 0;
	}
	@Override
	public int getNumXTiles() {
		return 0;
	}
	@Override
	public int getNumYTiles() {
		return 0;
	}
	@Override
	public int getMinTileX() {
		return 0;
	}
	@Override
	public int getMinTileY() {
		return 0;
	}
	@Override
	public int getTileWidth() {
		return 0;
	}
	@Override
	public int getTileHeight() {
		return 0;
	}
	@Override
	public int getTileGridXOffset() {
		return 0;
	}
	@Override
	public int getTileGridYOffset() {
		return 0;
	}
	@Override
	public Raster getTile(int tileX, int tileY) {
		return null;
	}
	@Override
	public Raster getData() {
		return null;
	}
	@Override
	public Raster getData(Rectangle rect) {
		return null;
	}
	@Override
	public WritableRaster copyData(WritableRaster raster) {
		return null;
	}
}