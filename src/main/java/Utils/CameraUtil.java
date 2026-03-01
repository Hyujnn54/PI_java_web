package Utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.stage.Window;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * CameraUtil — webcam capture helpers.
 * Requires optional dependency: webcam-capture (com.github.sarxos) + javafx-swing.
 * If not available, capture methods throw UnsupportedOperationException.
 */
public class CameraUtil {

    private static final boolean WEBCAM_AVAILABLE;

    static {
        boolean ok = false;
        try {
            Class.forName("com.github.sarxos.webcam.Webcam");
            ok = true;
        } catch (ClassNotFoundException ignored) {}
        WEBCAM_AVAILABLE = ok;
    }

    public static byte[] captureJpegBytesWithPreview(Window owner) throws Exception {
        if (!WEBCAM_AVAILABLE) throw new UnsupportedOperationException("webcam-capture library not installed.");
        return captureInternal(owner);
    }

    public static List<byte[]> captureMultipleJpegsWithPreview(Window owner, int count, int delayMs) throws Exception {
        if (!WEBCAM_AVAILABLE) throw new UnsupportedOperationException("webcam-capture library not installed.");
        return captureMultipleInternal(owner, count, delayMs);
    }

    public static byte[] cropToFace(byte[] jpgBytes, int left, int top, int right, int bottom) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpgBytes));
        if (img == null) return jpgBytes;

        int w = right - left;
        int h = bottom - top;
        if (w <= 0 || h <= 0) return jpgBytes;

        int marginX = (int)(w * 0.25);
        int marginY = (int)(h * 0.25);

        int x  = Math.max(0, left - marginX);
        int y  = Math.max(0, top  - marginY);
        int cw = Math.min(img.getWidth()  - x, w + marginX * 2);
        int ch = Math.min(img.getHeight() - y, h + marginY * 2);

        if (cw <= 0 || ch <= 0) return jpgBytes;
        return toJpegBytesHighQuality(img.getSubimage(x, y, cw, ch), 0.95f);
    }

    // ---- internal reflective calls (only reached when webcam-capture IS on classpath) ----

    @SuppressWarnings("unchecked")
    private static byte[] captureInternal(Window owner) throws Exception {
        // Use reflection so the class still compiles without the webcam-capture jar
        Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
        Object cam = webcamClass.getMethod("getDefault").invoke(null);
        if (cam == null) return null;

        openCam(webcamClass, cam);
        try {
            BufferedImage img = (BufferedImage) webcamClass.getMethod("getImage").invoke(cam);
            if (img == null) return null;
            return toJpegBytesHighQuality(img, 0.95f);
        } finally {
            webcamClass.getMethod("close").invoke(cam);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<byte[]> captureMultipleInternal(Window owner, int count, int delayMs) throws Exception {
        Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
        Object cam = webcamClass.getMethod("getDefault").invoke(null);
        List<byte[]> list = new ArrayList<>();
        if (cam == null) return list;

        openCam(webcamClass, cam);
        try {
            for (int i = 0; i < count; i++) {
                BufferedImage img = (BufferedImage) webcamClass.getMethod("getImage").invoke(cam);
                if (img != null) list.add(toJpegBytesHighQuality(img, 0.95f));
                Thread.sleep(delayMs);
            }
        } finally {
            webcamClass.getMethod("close").invoke(cam);
        }
        return list;
    }

    private static void openCam(Class<?> webcamClass, Object cam) throws Exception {
        webcamClass.getMethod("open").invoke(cam);
        // warm-up
        for (int i = 0; i < 5; i++) webcamClass.getMethod("getImage").invoke(cam);
    }

    public static boolean isWebcamAvailable() { return WEBCAM_AVAILABLE; }

    private static byte[] toJpegBytesHighQuality(BufferedImage img, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) { ImageIO.write(img, "jpg", baos); return baos.toByteArray(); }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.1f, Math.min(quality, 1.0f)));
        }
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(img, null, null), param);

        ios.close();
        writer.dispose();

        return baos.toByteArray();
    }
}