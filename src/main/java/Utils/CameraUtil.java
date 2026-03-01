package Utils;

import com.github.sarxos.webcam.Webcam;
import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CameraUtil — webcam capture helpers.
 * Requires optional dependency: webcam-capture (com.github.sarxos) + javafx-swing.
 * If not available, capture methods throw UnsupportedOperationException.
 */
public class CameraUtil {

    private static final boolean WEBCAM_AVAILABLE;
    static {
        boolean ok = false;
        try { Class.forName("com.github.sarxos.webcam.Webcam"); ok = true; }
        catch (ClassNotFoundException ignored) {}
        WEBCAM_AVAILABLE = ok;
    }

    public static boolean isWebcamAvailable() { return WEBCAM_AVAILABLE; }

    public static byte[] captureJpegBytesWithPreview(Window owner) throws Exception {
        if (!WEBCAM_AVAILABLE) throw new UnsupportedOperationException("webcam-capture not installed.");

        Webcam cam = Webcam.getDefault();
        if (cam == null) return null;

        Dimension wanted = new Dimension(1280, 720);
        if (cam.getViewSizes() != null) {
            boolean ok = false;
            for (Dimension d : cam.getViewSizes()) { if (d.equals(wanted)) { ok = true; break; } }
            cam.setViewSize(ok ? wanted : new Dimension(640, 480));
        } else {
            cam.setViewSize(new Dimension(640, 480));
        }

        cam.open();
        try {
            for (int i = 0; i < 10; i++) cam.getImage(); // warm-up

            AtomicReference<byte[]> result = new AtomicReference<>(null);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Camera - Capture face");

            ImageView view = new ImageView();
            view.setPreserveRatio(true);
            view.setFitWidth(640);

            Button btnCapture = new Button("Capture");
            Button btnCancel  = new Button("Cancel");
            HBox controls = new HBox(10, btnCapture, btnCancel);
            BorderPane root = new BorderPane(view);
            root.setBottom(controls);
            stage.setScene(new Scene(root, 700, 520));

            AnimationTimer timer = new AnimationTimer() {
                @Override public void handle(long now) {
                    BufferedImage img = cam.getImage();
                    if (img == null) return;
                    WritableImage fx = SwingFXUtils.toFXImage(img, null);
                    view.setImage(fx);
                }
            };
            timer.start();

            btnCancel.setOnAction(e -> { result.set(null); stage.close(); });
            btnCapture.setOnAction(e -> {
                try {
                    cam.getImage();
                    BufferedImage img = cam.getImage();
                    if (img == null) return;
                    result.set(toJpegBytesHighQuality(img, 0.95f));
                    stage.close();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            stage.setOnCloseRequest(e -> result.set(null));
            stage.showAndWait();
            timer.stop();
            return result.get();
        } finally {
            try { cam.close(); } catch (Exception ignored) {}
        }
    }

    public static List<byte[]> captureMultipleJpegsWithPreview(Window owner, int count, int delayMs) throws Exception {
        List<byte[]> list = new ArrayList<>();
        if (!WEBCAM_AVAILABLE) throw new UnsupportedOperationException("webcam-capture not installed.");

        Webcam cam = Webcam.getDefault();
        if (cam == null) return list;

        Dimension wanted = new Dimension(1280, 720);
        if (cam.getViewSizes() != null) {
            boolean ok = false;
            for (Dimension d : cam.getViewSizes()) { if (d.equals(wanted)) { ok = true; break; } }
            cam.setViewSize(ok ? wanted : new Dimension(640, 480));
        } else {
            cam.setViewSize(new Dimension(640, 480));
        }

        cam.open();
        try {
            for (int i = 0; i < 10; i++) cam.getImage(); // warm-up

            AtomicReference<List<byte[]>> result = new AtomicReference<>(null);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Camera - Blink / Move then Capture");

            ImageView view = new ImageView();
            view.setPreserveRatio(true);
            view.setFitWidth(640);

            Button btnCapture = new Button("Capture");
            Button btnCancel  = new Button("Cancel");
            HBox controls = new HBox(10, btnCapture, btnCancel);
            BorderPane root = new BorderPane(view);
            root.setBottom(controls);
            stage.setScene(new Scene(root, 700, 520));

            AnimationTimer timer = new AnimationTimer() {
                @Override public void handle(long now) {
                    BufferedImage img = cam.getImage();
                    if (img == null) return;
                    view.setImage(SwingFXUtils.toFXImage(img, null));
                }
            };
            timer.start();

            btnCancel.setOnAction(e -> { result.set(null); stage.close(); });
            btnCapture.setOnAction(e -> {
                new Thread(() -> {
                    try {
                        List<byte[]> shots = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            cam.getImage(); // discard
                            BufferedImage img = cam.getImage();
                            if (img != null) shots.add(toJpegBytesHighQuality(img, 0.95f));
                            Thread.sleep(delayMs);
                        }
                        result.set(shots);
                        javafx.application.Platform.runLater(stage::close);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        result.set(null);
                        javafx.application.Platform.runLater(stage::close);
                    }
                }).start();
            });
            stage.setOnCloseRequest(e -> result.set(null));
            stage.showAndWait();
            timer.stop();

            List<byte[]> shots = result.get();
            if (shots != null) list.addAll(shots);
            return list;
        } finally {
            try { cam.close(); } catch (Exception ignored) {}
        }
    }

    public static byte[] cropToFace(byte[] jpgBytes, int left, int top, int right, int bottom) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpgBytes));
        if (img == null) return jpgBytes;
        int w = right - left, h = bottom - top;
        if (w <= 0 || h <= 0) return jpgBytes;
        int marginX = (int)(w * 0.25), marginY = (int)(h * 0.25);
        int x  = Math.max(0, left - marginX);
        int y  = Math.max(0, top  - marginY);
        int cw = Math.min(img.getWidth()  - x, w + marginX * 2);
        int ch = Math.min(img.getHeight() - y, h + marginY * 2);
        if (cw <= 0 || ch <= 0) return jpgBytes;
        return toJpegBytesHighQuality(img.getSubimage(x, y, cw, ch), 0.95f);
    }

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