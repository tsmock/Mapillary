package org.openstreetmap.josm.plugins.mapillary.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryData;
import org.openstreetmap.josm.plugins.mapillary.MapillaryDataListener;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImportedImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.cache.MapillaryCache;
import org.openstreetmap.josm.tools.Shortcut;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.JPanel;

/**
 * Toggle dialog that shows an image and some buttons.
 * 
 * @author nokutu
 *
 */
public class MapillaryToggleDialog extends ToggleDialog implements
        ICachedLoaderListener, MapillaryDataListener {

    public final static String BASE_TITLE = "Mapillary picture";

    public static MapillaryToggleDialog INSTANCE;

    public volatile MapillaryAbstractImage image;

    public final SideButton nextButton = new SideButton(new nextPictureAction());
    public final SideButton previousButton = new SideButton(
            new previousPictureAction());
    public final SideButton redButton = new SideButton(new redAction());
    public final SideButton blueButton = new SideButton(new blueAction());

    private JPanel buttonsPanel;

    public MapillaryImageDisplay mapillaryImageDisplay;

    private MapillaryCache imageCache;
    private MapillaryCache thumbnailCache;

    public MapillaryToggleDialog() {
        super(tr(BASE_TITLE), "mapillary.png", tr("Open Mapillary window"),
                Shortcut.registerShortcut(tr("Mapillary dialog"),
                        tr("Open Mapillary main dialog"), KeyEvent.VK_M,
                        Shortcut.NONE), 200, false,
                MapillaryPreferenceSetting.class);
        MapillaryData.getInstance().addListener(this);
        addShortcuts();
        mapillaryImageDisplay = new MapillaryImageDisplay();

        blueButton.setForeground(Color.BLUE);
        redButton.setForeground(Color.RED);

        createLayout(
                mapillaryImageDisplay,
                Arrays.asList(new SideButton[] { blueButton, previousButton,
                        nextButton, redButton }),
                Main.pref.getBoolean("mapillary.reverse-buttons"));
        disableAllButtons();

    }

    /**
     * Adds the shortcuts to the buttons.
     */
    private void addShortcuts() {
        nextButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("PAGE_DOWN"), "next");
        nextButton.getActionMap().put("next", new nextPictureAction());
        previousButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("PAGE_UP"), "previous");
        previousButton.getActionMap().put("previous",
                new previousPictureAction());
        blueButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("control PAGE_UP"), "blue");
        blueButton.getActionMap().put("blue", new blueAction());
        redButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("control PAGE_DOWN"), "red");
        redButton.getActionMap().put("red", new redAction());
    }

    public static MapillaryToggleDialog getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MapillaryToggleDialog();
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    /**
     * Downloads the image of the selected MapillaryImage and sets in the
     * MapillaryImageDisplay object.
     */
    public synchronized void updateImage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateImage();
                }
            });
        } else {
            if (MapillaryLayer.INSTANCE == null) {
                return;
            }
            if (this.image == null) {
                mapillaryImageDisplay.setImage(null);
                setTitle(tr(BASE_TITLE));
                disableAllButtons();
                return;
            }
            if (image instanceof MapillaryImage) {
                mapillaryImageDisplay.hyperlink.setVisible(true);
                MapillaryImage mapillaryImage = (MapillaryImage) this.image;
                updateTitle();
                // Enables/disables next/previous buttons
                this.nextButton.setEnabled(false);
                this.previousButton.setEnabled(false);
                if (((MapillaryImage) image).getSequence() != null) {
                    MapillaryImage tempImage = (MapillaryImage) image;
                    while (tempImage.next() != null) {
                        tempImage = tempImage.next();
                        if (tempImage.isVisible()) {
                            this.nextButton.setEnabled(true);
                            break;
                        }
                    }
                }
                if (((MapillaryImage) image).getSequence() != null) {
                    MapillaryImage tempImage = (MapillaryImage) image;
                    while (tempImage.previous() != null) {
                        tempImage = tempImage.previous();
                        if (tempImage.isVisible()) {
                            this.previousButton.setEnabled(true);
                            break;
                        }
                    }
                }

                mapillaryImageDisplay.hyperlink.setURL(mapillaryImage.getKey());
                // Downloads the thumbnail.
                this.mapillaryImageDisplay.setImage(null);
                if (thumbnailCache != null)
                    thumbnailCache.cancelOutstandingTasks();
                thumbnailCache = new MapillaryCache(mapillaryImage.getKey(),
                        MapillaryCache.Type.THUMBNAIL);
                thumbnailCache.submit(this, false);

                // Downloads the full resolution image.
                if (imageCache != null)
                    imageCache.cancelOutstandingTasks();
                imageCache = new MapillaryCache(mapillaryImage.getKey(),
                        MapillaryCache.Type.FULL_IMAGE);
                imageCache.submit(this, false);
            } else if (image instanceof MapillaryImportedImage) {
                mapillaryImageDisplay.hyperlink.setVisible(false);
                this.nextButton.setEnabled(false);
                this.previousButton.setEnabled(false);
                MapillaryImportedImage mapillaryImage = (MapillaryImportedImage) this.image;
                try {
                    mapillaryImageDisplay.setImage(mapillaryImage.getImage());
                } catch (IOException e) {
                    Main.error(e);
                }
                mapillaryImageDisplay.hyperlink.setURL(null);
            }
        }
    }

    private void disableAllButtons() {
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);
        blueButton.setEnabled(false);
        redButton.setEnabled(false);
        mapillaryImageDisplay.hyperlink.setVisible(false);
    }

    /**
     * Sets a new MapillaryImage to be shown.
     * 
     * @param image
     */
    public synchronized void setImage(MapillaryAbstractImage image) {
        this.image = image;
    }

    public synchronized void updateTitle() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateTitle();
                }
            });
        } else {
            if (this.image != null) {
                MapillaryImage mapillaryImage = (MapillaryImage) this.image;
                String title = tr(BASE_TITLE);
                if (mapillaryImage.getUser() != null)
                    title += " -- " + mapillaryImage.getUser();
                if (mapillaryImage.getCapturedAt() != 0)
                    title += " -- " + mapillaryImage.getDate();
                setTitle(title);
            }
        }
    }

    /**
     * Returns the MapillaryImage objects which is being shown.
     * 
     * @return
     */
    public synchronized MapillaryAbstractImage getImage() {
        return this.image;
    }

    /**
     * Action class form the next image button.
     * 
     * @author Jorge
     *
     */
    class nextPictureAction extends AbstractAction {
        public nextPictureAction() {
            putValue(NAME, tr("Next picture"));
            putValue(SHORT_DESCRIPTION,
                    tr("Shows the next picture in the sequence"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (MapillaryToggleDialog.getInstance().getImage() != null) {
                MapillaryData.getInstance().selectNext();
            }
        }
    }

    /**
     * Action class for the previous image button.
     * 
     * @author Jorge
     *
     */
    class previousPictureAction extends AbstractAction {
        public previousPictureAction() {
            putValue(NAME, tr("Previous picture"));
            putValue(SHORT_DESCRIPTION,
                    tr("Shows the previous picture in the sequence"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (MapillaryToggleDialog.getInstance().getImage() != null) {
                MapillaryData.getInstance().selectPrevious();
            }
        }
    }

    /**
     * Action class to jump to the image following the red line.
     * 
     * @author nokutu
     *
     */
    class redAction extends AbstractAction {
        public redAction() {
            putValue(NAME, tr("Jump to red"));
            putValue(
                    SHORT_DESCRIPTION,
                    tr("Jumps to the picture at the other side of the red line"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (MapillaryToggleDialog.getInstance().getImage() != null) {
                MapillaryData.getInstance().setSelectedImage(
                        MapillaryLayer.RED, true);
            }
        }
    }

    /**
     * Action class to jump to the image following the blue line.
     * 
     * @author nokutu
     *
     */
    class blueAction extends AbstractAction {
        public blueAction() {
            putValue(NAME, tr("Jump to blue"));
            putValue(
                    SHORT_DESCRIPTION,
                    tr("Jumps to the picture at the other side of the blue line"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (MapillaryToggleDialog.getInstance().getImage() != null) {
                MapillaryData.getInstance().setSelectedImage(
                        MapillaryLayer.BLUE, true);
            }
        }
    }

    /**
     * When the pictures are returned from the cache, they are set in the
     * {@link MapillaryImageDisplay} object.
     */
    @Override
    public void loadingFinished(final CacheEntry data,
            final CacheEntryAttributes attributes, final LoadResult result) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadingFinished(data, attributes, result);
                }
            });
        } else if (data != null && result == LoadResult.SUCCESS) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data
                        .getContent()));
                if (img == null)
                    return;
                if (this.mapillaryImageDisplay.getImage() == null)
                    mapillaryImageDisplay.setImage(img);
                else if (img.getHeight() > this.mapillaryImageDisplay
                        .getImage().getHeight()) {
                    mapillaryImageDisplay.setImage(img);
                }
            } catch (IOException e) {
                Main.error(e);
            }
        }
    }

    /**
     * Creates the layout of the dialog.
     * 
     * @param data
     *            The content of the dialog
     * @param buttons
     *            The buttons where you can click
     * @param reverse
     *            {@code true} if the buttons should go at the top;
     *            {@code false} otherwise.
     */
    public void createLayout(Component data, List<SideButton> buttons,
            boolean reverse) {
        this.removeAll();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(data, BorderLayout.CENTER);
        if (reverse) {
            buttonsPanel = new JPanel(new GridLayout(1, 1));
            if (!buttons.isEmpty() && buttons.get(0) != null) {
                final JPanel buttonRowPanel = new JPanel(Main.pref.getBoolean(
                        "dialog.align.left", false) ? new FlowLayout(
                        FlowLayout.LEFT) : new GridLayout(1, buttons.size()));
                buttonsPanel.add(buttonRowPanel);
                for (SideButton button : buttons)
                    buttonRowPanel.add(button);
            }
            panel.add(buttonsPanel, BorderLayout.NORTH);
            createLayout(panel, true, null);
        } else
            createLayout(panel, true, buttons);
        this.add(titleBar, BorderLayout.NORTH);
    }

    @Override
    public void selectedImageChanged(MapillaryAbstractImage oldImage,
            MapillaryAbstractImage newImage) {
        setImage(MapillaryData.getInstance().getSelectedImage());
        updateImage();
    }

    @Override
    public void imagesAdded() {
    }
}
