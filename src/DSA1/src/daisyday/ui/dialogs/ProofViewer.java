package DSA1.src.daisyday.ui.dialogs;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;

/** Opens an attached proof: pictures in a preview window, other files in the default app. */
public final class ProofViewer {

    static final Set<String> IMAGE_TYPES = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private static final int PREVIEW_WIDTH = 560;
    private static final int PREVIEW_HEIGHT = 380;

    private ProofViewer() {
    }

    public static void view(Component owner, File file) {
        if (file == null || !file.isFile()) {
            Modal.warning(owner, "This file can't be found anymore.\nIt may have been moved or deleted.");
            return;
        }

        if (IMAGE_TYPES.contains(extension(file))) {
            BufferedImage image = readImage(file);
            if (image != null) {
                preview(owner, file, image);
                return;
            }
        }
        openWithSystem(owner, file);
    }

    static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // ---------- picture preview ----------

    private static void preview(Component owner, File file, BufferedImage image) {
        double scale = Math.min(1.0, Math.min(
                PREVIEW_WIDTH / (double) image.getWidth(), PREVIEW_HEIGHT / (double) image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        JLabel picture = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        picture.setBorder(BorderFactory.createLineBorder(new Color(229, 224, 224), 1));

        JLabel name = new JLabel(file.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        name.setForeground(HEADER);
        JLabel size = new JLabel(image.getWidth() + " × " + image.getHeight() + " px  ·  "
                + ProofCard.formatSize(file.length()));
        size.setFont(new Font("SansSerif", Font.PLAIN, 11));
        size.setForeground(MUTED);

        JPanel caption = new JPanel(new BorderLayout(0, 2));
        caption.setOpaque(false);
        caption.add(name, BorderLayout.NORTH);
        caption.add(size, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        body.add(caption, BorderLayout.NORTH);
        body.add(picture, BorderLayout.CENTER);

        JButton close = Modal.primaryButton("Close");
        JButton open = Modal.secondaryButton("Open File");
        JDialog dialog = Modal.create(owner, "Proof", body, Modal.footerOf(null, open, close), close);
        close.addActionListener(event -> dialog.dispose());
        open.addActionListener(event -> openWithSystem(dialog, file));
        dialog.setVisible(true); // blocks until closed
    }

    // ---------- other files ----------

    private static void openWithSystem(Component owner, File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Modal.info(owner, "Can't Open File",
                    "This computer can't open files from here.\nThe file is saved at:\n" + file.getAbsolutePath());
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException | RuntimeException e) {
            Modal.error(owner, "The file could not be opened.\n" + file.getAbsolutePath());
        }
    }
}
