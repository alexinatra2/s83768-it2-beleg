import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class JpegDisplay extends JpegDisplayDemo {
    private static final int SLICE_HEIGHT = 16;

    @Override
    BufferedImage setTransparency(BufferedImage back, BufferedImage foreground, List<Integer> list) {
        BufferedImage newImage = new BufferedImage(
                back.getWidth(),
                back.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = newImage.createGraphics();

        // Draw the foreground image
        g.drawImage(foreground, 0, 0, null);

        // Set composite to CLEAR for transparency
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));

        // Clear each horizontal slice indicated in the list
        list.forEach(index -> {
            int y = index * SLICE_HEIGHT;
            g.fillRect(0, y, back.getWidth(), SLICE_HEIGHT);
        });

        // Restore composite to default
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g.dispose();

        Graphics2D g2 = back.createGraphics();

        g2.drawImage(newImage, 0, 0, null);

        return back;
    }
}
