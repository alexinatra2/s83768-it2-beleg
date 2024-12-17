import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class JpegDisplay extends JpegDisplayDemo {
    @Override
    BufferedImage setTransparency(BufferedImage back, BufferedImage foreground, List<Integer> list) {
        BufferedImage combined = new BufferedImage(
                foreground.getWidth(),
                foreground.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = combined.createGraphics();

        g.drawImage(foreground, 0, 0, null);

        int sliceHeight = foreground.getHeight() / 8;
        for (int slice : list) {
            int yStart = slice * sliceHeight;
            int yEnd = Math.min(yStart + sliceHeight, foreground.getHeight());
            for (int y = yStart; y < yEnd; y++) {
                for (int x = 0; x < foreground.getWidth(); x++) {
                    combined.setRGB(x, y, back.getRGB(x, y));
                }
            }
        }

        g.dispose();
        return combined;
    }
}
