package DSA1.src.daisyday.ui.dialogs;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

import static DSA1.src.daisyday.ui.Theme.GREEN;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;
import static DSA1.src.daisyday.ui.UiFactory.tag;

/**
 * The proof section of the task details: a card with a preview, the file name and size,
 * and View / Replace / Remove. Once the task is completed the card is locked: the proof
 * can still be viewed but not changed.
 */
public final class ProofCard extends JPanel {

    private static final Color LINE = new Color(229, 224, 224);
    private static final Color SOFT = new Color(250, 247, 247);
    private static final Color RED = new Color(190, 30, 30);
    private static final int NAME_LIMIT = 26;
    private static final long PREVIEW_LIMIT = 20L * 1024 * 1024;

    private final Component owner;
    private final boolean locked;
    private File file;
    private Consumer<File> onChange = f -> { };

    public ProofCard(Component owner, boolean locked, File file) {
        super(new BorderLayout());
        this.owner = owner;
        this.locked = locked;
        this.file = file;
        setOpaque(false);
        setPreferredSize(new Dimension(300, file == null ? 96 : 112));
        rebuild(false);
    }

    /** The attached file, or null. */
    public File getFile() {
        return file;
    }

    /** Called after the user attaches, replaces or removes a file (the card's size may change). */
    public void setChangeListener(Consumer<File> listener) {
        this.onChange = listener;
    }

    private void setFile(File selected) {
        this.file = selected;
        setPreferredSize(new Dimension(300, selected == null ? 96 : 112));
        rebuild(true);
    }

    private void rebuild(boolean notify) {
        removeAll();
        add(file == null ? buildEmpty() : buildFilled(), BorderLayout.CENTER);
        revalidate();
        repaint();
        if (notify) {
            onChange.accept(file);
        }
    }

    // ---------- no file ----------

    private JPanel buildEmpty() {
        JLabel title = new JLabel(locked ? "No proof was attached" : "No proof attached yet");
        title.setFont(new Font("Serif", Font.PLAIN, 15));
        title.setForeground(HEADER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(SOFT);
        box.setBorder(BorderFactory.createCompoundBorder(
                new DashedBorder(new Color(200, 190, 190)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        box.add(Box.createVerticalGlue());
        box.add(title);

        if (!locked) {
            JLabel hint = new JLabel("Attach a file before you complete this task.");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            hint.setForeground(MUTED);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton attach = small(Modal.primaryButton("Attach Proof  ▾"));
            attach.setAlignmentX(Component.CENTER_ALIGNMENT);
            attach.addActionListener(event -> chooserMenu().show(attach, 0, attach.getHeight()));

            box.add(Box.createVerticalStrut(3));
            box.add(hint);
            box.add(Box.createVerticalStrut(9));
            box.add(attach);
        }
        box.add(Box.createVerticalGlue());
        return box;
    }

    // ---------- file attached ----------

    private JPanel buildFilled() {
        boolean exists = file.isFile();

        JLabel name = new JLabel(shortName(file.getName(), NAME_LIMIT));
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        name.setForeground(exists ? HEADER : RED);
        name.setToolTipText(file.getAbsolutePath());

        JLabel meta = new JLabel(exists ? describe(file) : "File not found");
        meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meta.setForeground(exists ? MUTED : RED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(Box.createVerticalGlue());
        text.add(name);
        text.add(Box.createVerticalStrut(3));
        text.add(meta);
        text.add(Box.createVerticalGlue());

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(new Thumb(file, exists), BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        if (locked) {
            JPanel tagBox = new JPanel(new BorderLayout());
            tagBox.setOpaque(false);
            tagBox.add(tag("SUBMITTED", GREEN), BorderLayout.NORTH);
            header.add(tagBox, BorderLayout.EAST);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);

        JButton view = small(Modal.primaryButton("View"));
        view.setEnabled(exists);
        view.addActionListener(event -> ProofViewer.view(owner, file));
        buttons.add(view);

        if (locked) {
            JLabel note = new JLabel("Locked: the task is completed");
            note.setFont(new Font("SansSerif", Font.PLAIN, 11));
            note.setForeground(MUTED);
            buttons.add(Box.createHorizontalStrut(4));
            buttons.add(note);
        } else {
            JButton replace = small(Modal.secondaryButton("Replace  ▾"));
            replace.addActionListener(event -> chooserMenu().show(replace, 0, replace.getHeight()));
            JButton remove = small(Modal.secondaryButton("Remove"));
            remove.addActionListener(event -> setFile(null));
            buttons.add(replace);
            buttons.add(remove);
        }

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(SOFT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.add(header, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    // ---------- choosing a file ----------

    private JPopupMenu chooserMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem anyFile = new JMenuItem("Attach File");
        JMenuItem photo = new JMenuItem("Photo Library");
        anyFile.addActionListener(event -> choose(false));
        photo.addActionListener(event -> choose(true));
        menu.add(anyFile);
        menu.add(photo);
        return menu;
    }

    private void choose(boolean imagesOnly) {
        if (locked) {
            return; // a completed task keeps the proof it was completed with
        }

        JFileChooser chooser = new JFileChooser();
        if (imagesOnly) {
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif"));
        }

        if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (!selected.isFile()) {
                Modal.warning(owner, "Please select an existing file.");
                return;
            }
            setFile(selected);
        }
    }

    // ---------- helpers ----------

    private static JButton small(JButton button) {
        Color border = button.getBackground().equals(Color.WHITE) ? new Color(200, 190, 190) : HEADER;
        button.setFont(new Font("Serif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        return button;
    }

    /** "very-long-file-name.png" becomes "very-long-file-na....png". */
    static String shortName(String name, int limit) {
        if (name.length() <= limit) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String extension = dot > 0 && name.length() - dot <= 6 ? name.substring(dot) : "";
        int keep = Math.max(4, limit - extension.length() - 3);
        return name.substring(0, keep) + "..." + extension;
    }

    static String describe(File file) {
        String extension = ProofViewer.extension(file);
        String kind = switch (extension) {
            case "png", "jpg", "jpeg", "gif", "bmp" -> extension.toUpperCase(Locale.ROOT) + " image";
            case "pdf" -> "PDF document";
            case "doc", "docx" -> "Word document";
            case "xls", "xlsx" -> "Excel sheet";
            case "ppt", "pptx" -> "Slide deck";
            case "txt" -> "Text file";
            case "zip", "rar", "7z" -> "Archive";
            case "" -> "File";
            default -> extension.toUpperCase(Locale.ROOT) + " file";
        };
        return kind + "  ·  " + formatSize(file.length());
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ---------- preview square ----------

    /** A small picture of the file: the image itself, or the file type on a maroon square. */
    private static final class Thumb extends JComponent {

        private static final int SIZE = 52;

        private final BufferedImage image;
        private final String badge;
        private final boolean exists;

        Thumb(File file, boolean exists) {
            this.exists = exists;
            String extension = ProofViewer.extension(file);
            this.badge = !exists ? "!" : extension.isEmpty() ? "FILE"
                    : extension.substring(0, Math.min(4, extension.length())).toUpperCase(Locale.ROOT);
            this.image = exists && ProofViewer.IMAGE_TYPES.contains(extension) && file.length() <= PREVIEW_LIMIT
                    ? ProofViewer.readImage(file) : null;
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, SIZE - 1, SIZE - 1, 10, 10);

            if (image != null) {
                // fill the square with the middle of the picture
                double scale = Math.max(SIZE / (double) image.getWidth(), SIZE / (double) image.getHeight());
                int w = (int) Math.round(image.getWidth() * scale);
                int h = (int) Math.round(image.getHeight() * scale);
                g.setClip(shape);
                g.drawImage(image, (SIZE - w) / 2, (SIZE - h) / 2, w, h, null);
                g.setClip(null);
                g.setColor(LINE);
                g.setStroke(new BasicStroke(1f));
                g.draw(shape);
            } else {
                g.setColor(exists ? HEADER : new Color(235, 215, 215));
                g.fill(shape);
                g.setColor(exists ? Color.WHITE : RED);
                g.setFont(new Font("SansSerif", Font.BOLD, badge.length() > 3 ? 11 : 13));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(badge, (SIZE - fm.stringWidth(badge)) / 2, (SIZE + fm.getAscent()) / 2 - 2);
            }
            g.dispose();
        }
    }

    // ---------- dashed outline ----------

    private static final class DashedBorder extends AbstractBorder {

        private final Color color;

        DashedBorder(Color color) {
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{5f, 4f}, 0f));
            g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1, height - 1, 10, 10));
            g.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(2, 2, 2, 2);
        }
    }
}
