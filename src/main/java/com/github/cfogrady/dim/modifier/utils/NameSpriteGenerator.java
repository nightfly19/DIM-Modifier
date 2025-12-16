package com.github.cfogrady.dim.modifier.utils;

import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Slf4j
public class NameSpriteGenerator {
    private static final String FONTS_DIR = "/fonts/";
    private static final int CHAR_HEIGHT = 15;
    private static final Color GREEN = Color.LIME; // 0, 255, 0
    private static final Color WHITE = Color.WHITE; // 255, 255, 255

    private final Map<Character, Image> charMap = new HashMap<>();
    private Image charSheet;
    private String currentFontName;
    private Color currentBackgroundColor;

    public NameSpriteGenerator() {
        // No default load
    }

    public List<String> getAvailableFonts() {
        List<String> fonts = new ArrayList<>();
        try {
            // 1. Load built-in fonts from resources
            java.net.URL url = getClass().getResource(FONTS_DIR);
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    java.io.File folder = new java.io.File(url.toURI());
                    java.io.File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                    if (files != null) {
                        for (java.io.File f : files) {
                            fonts.add(f.getName());
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    // Start of Jar support if needed, but for now we rely on FileSystem for 'add'
                    // For listing, we might need to scan the jar.
                    // However, we now have a custom folder.
                }
            }

            // 2. Load custom fonts from AppData/Home
            java.io.File customDir = getCustomFontsDir();
            if (customDir.exists() && customDir.isDirectory()) {
                java.io.File[] customFiles = customDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (customFiles != null) {
                    for (java.io.File f : customFiles) {
                        if (!fonts.contains(f.getName())) { // Avoid duplicates
                            fonts.add(f.getName());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error listing fonts", e);
        }
        return fonts;
    }

    private java.io.File getCustomFontsDir() {
        String os = System.getProperty("os.name").toLowerCase();
        java.io.File baseDir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                baseDir = new java.io.File(appData, "DIM-Modifier");
            } else {
                baseDir = new java.io.File(System.getProperty("user.home"), "DIM-Modifier");
            }
        } else {
            baseDir = new java.io.File(System.getProperty("user.home"), ".dim-modifier");
        }
        return new java.io.File(baseDir, "fonts");
    }

    public void addFont(java.io.File sourceFile) {
        try {
            java.io.File fontsDir = getCustomFontsDir();
            if (!fontsDir.exists()) {
                if (!fontsDir.mkdirs()) {
                    log.error("Failed to create fonts directory: {}", fontsDir.getAbsolutePath());
                    return;
                }
            }

            java.io.File destFile = new java.io.File(fontsDir, sourceFile.getName());
            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Added font to custom directory: {}", destFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error adding font", e);
        }
    }

    public void loadFont(String fontName) {
        if (fontName == null || fontName.equals(currentFontName))
            return;

        currentFontName = fontName; // Set name first for logging

        // Try loading from custom dir first
        java.io.File customFile = new java.io.File(getCustomFontsDir(), fontName);
        if (customFile.exists()) {
            try {
                charSheet = new Image(customFile.toURI().toString());
                initializeCharMap();
                return;
            } catch (Exception e) {
                log.error("Failed to load custom font: " + fontName, e);
            }
        }

        // Fallback to resources
        String path = FONTS_DIR + fontName;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.error("Font Sprite Sheet missing: {}", path);
                return;
            }
            charSheet = new Image(is);
            initializeCharMap();
        } catch (IOException e) {
            log.error("Failed to load font sprite sheet: " + fontName, e);
        }
    }

    public boolean isResourceLoaded() {
        return charSheet != null;
    }

    private void initializeCharMap() {
        charMap.clear();

        // Define the standard character order expected in the sprite sheet
        // Note: Space is handled separately as it is usually transparent
        String charOrder = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-:.,()1234567890";

        PixelReader reader = charSheet.getPixelReader();
        int width = (int) charSheet.getWidth();
        int height = (int) charSheet.getHeight();

        // Detect background color by finding the most frequent color
        currentBackgroundColor = detectBackgroundColor(reader, width, height);
        log.info("Detected background color for {}: {}", currentFontName, currentBackgroundColor);

        List<javafx.scene.image.WritableImage> detectedChars = new ArrayList<>();

        int startX = -1;
        for (int x = 0; x < width; x++) {
            boolean isColumnVisible = false;
            for (int y = 0; y < height; y++) {
                if (isVisible(reader.getColor(x, y))) {
                    isColumnVisible = true;
                    break;
                }
            }

            if (isColumnVisible) {
                if (startX == -1) {
                    startX = x;
                }
            } else {
                if (startX != -1) {
                    // End of character
                    int charWidth = x - startX;
                    javafx.scene.image.WritableImage cropped = new javafx.scene.image.WritableImage(reader, startX, 0,
                            charWidth, height);
                    detectedChars.add(cropped);
                    startX = -1;
                }
            }
        }
        // Handle last char if image ends with visible pixels
        if (startX != -1) {
            int charWidth = width - startX;
            javafx.scene.image.WritableImage cropped = new javafx.scene.image.WritableImage(reader, startX, 0,
                    charWidth, height);
            detectedChars.add(cropped);
        }

        log.info("Detected {} characters in font {}", detectedChars.size(), currentFontName);

        // Fallback if detection failed (e.g. solid block detected or too few chars)
        // We expect around 68 characters. If we find significantly fewer, something is
        // wrong.
        if (detectedChars.size() < 40) {
            log.warn("Gap-based auto-detection failed (found {} chars). Trying Top-Row Marker detection...",
                    detectedChars.size());
            detectedChars = detectCharsByTopRowMarkers(reader, width, height, currentBackgroundColor);

            if (detectedChars.size() < 40) {
                log.warn(
                        "Top-Row Marker detection failed (found {} chars). Falling back to standard fixed coordinates.",
                        detectedChars.size());
                initializeStandardCharMap();
                return;
            } else {
                log.info("Top-Row Marker detection successful (found {} chars).", detectedChars.size());
            }
        }

        // Map detected chars to expected chars
        int detectedIdx = 0;
        for (char c : charOrder.toCharArray()) {
            if (detectedIdx < detectedChars.size()) {
                charMap.put(c, detectedChars.get(detectedIdx));
                detectedIdx++;
            } else {
                log.warn("Not enough characters detected in font {}. Missing: {}", currentFontName, c);
            }
        }

        // Handle Space separately (create a blank 4px wide image)
        javafx.scene.image.WritableImage spaceImg = new javafx.scene.image.WritableImage(4, height);
        // Fill with transparent? It is already transparent by default.
        charMap.put(' ', spaceImg);
    }

    private void initializeStandardCharMap() {
        charMap.clear();
        // Upper case
        addChar('A', 0, 0, 8);
        addChar('B', 9, 0, 7);
        addChar('C', 17, 0, 7);
        addChar('D', 25, 0, 7);
        addChar('E', 33, 0, 6);
        addChar('F', 40, 0, 6);
        addChar('G', 47, 0, 7);
        addChar('H', 55, 0, 8);
        addChar('I', 64, 0, 3);
        addChar('J', 68, 0, 7);
        addChar('K', 76, 0, 7);
        addChar('L', 84, 0, 6);
        addChar('M', 91, 0, 11);
        addChar('N', 103, 0, 11);
        addChar('O', 115, 0, 8);
        addChar('P', 124, 0, 7);
        addChar('Q', 132, 0, 8);
        addChar('R', 141, 0, 7);
        addChar('S', 149, 0, 8);
        addChar('T', 158, 0, 7);
        addChar('U', 166, 0, 8);
        addChar('V', 175, 0, 8);
        addChar('W', 184, 0, 11);
        addChar('X', 196, 0, 7);
        addChar('Y', 204, 0, 7);
        addChar('Z', 212, 0, 7);

        // Lower case
        addChar('a', 220, 0, 6);
        addChar('b', 227, 0, 6);
        addChar('c', 234, 0, 6);
        addChar('d', 241, 0, 6);
        addChar('e', 248, 0, 6);
        addChar('f', 255, 0, 5);
        addChar('g', 261, 0, 7);
        addChar('h', 269, 0, 7);
        addChar('i', 277, 0, 2);
        addChar('j', 280, 0, 4);
        addChar('k', 285, 0, 6);
        addChar('l', 292, 0, 2);
        addChar('m', 295, 0, 10);
        addChar('n', 306, 0, 7);
        addChar('o', 314, 0, 6);
        addChar('p', 321, 0, 6);
        addChar('q', 328, 0, 6);
        addChar('r', 335, 0, 6);
        addChar('s', 342, 0, 6);
        addChar('t', 349, 0, 5);
        addChar('u', 355, 0, 6);
        addChar('v', 362, 0, 6);
        addChar('w', 369, 0, 10);
        addChar('x', 380, 0, 7);
        addChar('y', 388, 0, 8);
        addChar('z', 397, 0, 6);

        // Symbols
        addChar(' ', 404, 0, 3);
        addChar('-', 408, 0, 3);
        addChar(':', 412, 0, 2);
        addChar('.', 415, 0, 2);
        addChar('(', 418, 0, 4);
        addChar(')', 423, 0, 4);

        // Numbers
        addChar('1', 428, 0, 5);
        addChar('2', 434, 0, 7);
        addChar('3', 442, 0, 6);
        addChar('4', 449, 0, 6);
        addChar('5', 456, 0, 7);
        addChar('6', 464, 0, 7);
        addChar('7', 472, 0, 7);
        addChar('8', 480, 0, 6);
        addChar('9', 487, 0, 6);
        addChar('0', 494, 0, 7);
    }

    private void addChar(char c, int x, int y, int width) {
        PixelReader reader = charSheet.getPixelReader();
        javafx.scene.image.WritableImage cropped = new javafx.scene.image.WritableImage(reader, x, y, width,
                CHAR_HEIGHT);
        charMap.put(c, cropped);
    }

    private List<javafx.scene.image.WritableImage> detectCharsByTopRowMarkers(PixelReader reader, int width, int height,
            Color bgColor) {
        List<javafx.scene.image.WritableImage> chars = new ArrayList<>();
        int startX = -1;

        // User logic: "division is the inverted color of the initial pixel"
        Color initialPixel = reader.getColor(0, 0);
        Color markerColor = invertColor(initialPixel);
        log.info("Top-Row Marker Detection: Initial Pixel: {}, Marker Color: {}", initialPixel, markerColor);

        for (int x = 0; x < width; x++) {
            Color c = reader.getColor(x, 0);
            boolean isMarker = isSameColor(c, markerColor);

            if (!isMarker) {
                if (startX == -1) {
                    startX = x;
                }
            } else {
                if (startX != -1) {
                    // End of character
                    int charWidth = x - startX;
                    if (charWidth > 0) {
                        javafx.scene.image.WritableImage cropped = new javafx.scene.image.WritableImage(reader, startX,
                                0, charWidth, height);
                        chars.add(cropped);
                    }
                    startX = -1;
                }
            }
        }
        // Handle last char
        if (startX != -1) {
            int charWidth = width - startX;
            if (charWidth > 0) {
                javafx.scene.image.WritableImage cropped = new javafx.scene.image.WritableImage(reader, startX, 0,
                        charWidth, height);
                chars.add(cropped);
            }
        }
        return chars;
    }

    private Color invertColor(Color c) {
        // Invert RGB components, keep opacity
        return new Color(1.0 - c.getRed(), 1.0 - c.getGreen(), 1.0 - c.getBlue(), c.getOpacity());
    }

    private Color detectBackgroundColor(PixelReader reader, int width, int height) {
        Map<Color, Integer> colorCounts = new HashMap<>();
        // Sample pixels to find the most frequent one
        // We don't need to check every pixel, checking a grid is enough
        int stepX = Math.max(1, width / 50);
        int stepY = Math.max(1, height / 10);

        for (int x = 0; x < width; x += stepX) {
            for (int y = 0; y < height; y += stepY) {
                Color c = reader.getColor(x, y);
                colorCounts.put(c, colorCounts.getOrDefault(c, 0) + 1);
            }
        }

        Color mostFrequent = null;
        int maxCount = -1;

        for (Map.Entry<Color, Integer> entry : colorCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        return mostFrequent;
    }

    public SpriteData.Sprite generateNameSprite(String name, String fontName) {
        if (fontName != null) {
            loadFont(fontName);
        }

        if (!isResourceLoaded()) {
            throw new IllegalStateException("Font resource not loaded.");
        }

        // Validate characters
        for (char c : name.toCharArray()) {
            if (!charMap.containsKey(c)) {
                throw new IllegalArgumentException("Unsupported character: " + c);
            }
        }

        int canvasWidth = 80;
        BufferedImage canvas = new BufferedImage(canvasWidth, CHAR_HEIGHT, BufferedImage.TYPE_INT_RGB);
        fillGreen(canvas);

        int x = 1;
        for (char c : name.toCharArray()) {
            Image charImg = charMap.get(c);
            int charWidth = (int) charImg.getWidth();

            if (x + charWidth > canvasWidth) {
                // Expand canvas
                BufferedImage newCanvas = new BufferedImage(canvasWidth + 80, CHAR_HEIGHT, BufferedImage.TYPE_INT_RGB);
                fillGreen(newCanvas);
                newCanvas.getGraphics().drawImage(canvas, 0, 0, null);
                canvas = newCanvas;
                canvasWidth += 80;
            }

            // Kerning logic (simplified from python script)
            // The python script checks for white pixels to determine overlap.
            // Here we will just implement the basic logic:
            // Iterate y from 3 to 12. If char pixel is white, check if canvas has white at
            // x-2.
            // If collision, x++. Else y++.
            // This is complex to do with JavaFX Image and AWT BufferedImage mixed.
            // Let's convert JavaFX Image to AWT BufferedImage for easier pixel manipulation
            // or just use AWT for everything?
            // Since the output is SpriteData (byte array), we eventually need raw pixels.

            // Let's stick to the Python logic.
            // We need to read pixels from the char image.
            PixelReader reader = charImg.getPixelReader();

            int y = 3;
            while (y <= 12) {
                Color firstColColor = reader.getColor(0, y);
                // Python: if letters[elem].getpixel((0, y)) != white: continue
                // Meaning: If it is NOT White (assuming White=Text), then it is Background.
                // Continue.
                // So: If Background, Continue.
                // My isVisible returns true for Text.
                // So if !isVisible (Background), Continue.
                if (!isVisible(firstColColor)) {
                    y++;
                    continue;
                }

                // Python: if namesprite.getpixel((x-2, y)) == white: x+=1
                // Meaning: If collision with Text at x-2.
                // If we want 1px gap, we should ensure no text at x-1.
                // So checkCollision at x-1.
                if (checkCollision(canvas, x - 1, y)) {
                    x++;
                } else {
                    y++;
                }
            }

            // Paste character
            pasteImage(canvas, charImg, x, 0);
            x += charWidth;
        }

        // MON suffix handling
        if (name.endsWith("MON")) {
            // Calculate text width
            int textWidth = calculateTextWidth(canvas);
            int lmargin = calculateMargin(canvasWidth, textWidth, name);

            if (name.endsWith("MON")) {
                if ((textWidth + 2 + lmargin) > (canvasWidth - 3)) {
                    int monX;
                    if ((textWidth + 2 + lmargin) == (canvasWidth - 1)) {
                        monX = canvasWidth - 36;
                    } else {
                        monX = canvasWidth - 37;
                    }
                    // Paste M O N with specific spacing
                    // This overwrites previous MON? The python script seems to paste it over.
                    // But wait, we already pasted MON in the loop.
                    // The python script pastes it AGAIN at a specific position?
                    // Yes: namesprite.paste(letters["M"], (x, 0)) etc.

                    // Let's replicate:
                    pasteImage(canvas, charMap.get('M'), monX, 0);
                    pasteImage(canvas, charMap.get(' '), monX + 12, 0); // Space? Python script uses space from
                                                                        // charsheet
                    pasteImage(canvas, charMap.get('O'), monX + 13, 0);
                    pasteImage(canvas, charMap.get(' '), monX + 23, 0);
                    pasteImage(canvas, charMap.get('N'), monX + 24, 0);
                    pasteImage(canvas, charMap.get(' '), monX + 34, 0);
                }
            }
        }

        // Center/Margin logic
        int textWidth = calculateTextWidth(canvas);
        int lmargin = calculateMargin(canvasWidth, textWidth, name);

        BufferedImage finalCanvas = new BufferedImage(canvasWidth, CHAR_HEIGHT, BufferedImage.TYPE_INT_RGB);
        fillGreen(finalCanvas);
        finalCanvas.getGraphics().drawImage(canvas, lmargin, 0, null);

        return convertToSprite(finalCanvas);
    }

    private boolean isText(Color color) {
        // Assume text is not transparent and not the green background color we use for
        // canvas
        // Also check if it's close to the text color if we can determine it.
        // For robustness with different sprite sheets, let's assume anything with high
        // opacity and not green is text.
        // Or better, let's stick to the Python script's "White" but make it "Not
        // Background".
        // If the sprite sheet has alpha, text is opaque.
        return color.getOpacity() > 0.1;
    }

    private boolean isText(int rgb) {
        // Check alpha or specific color.
        // In BufferedImage (TYPE_INT_RGB), alpha is ignored/255.
        // We need to check against the background color of the sprite sheet.
        // But we cropped it.
        // Let's assume the sprite sheet background is transparent or black/white.
        // If we use the JavaFX Image PixelReader, we have Alpha.
        return true; // Placeholder, logic moved to checkCollision/main loop
    }

    // Updated loop in generateNameSprite
    /*
     * int y = 3;
     * while (y <= 12) {
     * Color firstColColor = reader.getColor(0, y);
     * // If this pixel is TEXT, we skip collision check (per Python logic
     * interpretation? or my fix?)
     * // Wait, if Python says "if pixel != white (background): continue", it means
     * "If it is Text, continue".
     * // This means we DON'T check collision if the new char has text on the edge.
     * // This seems to imply we allow the text to overlap the previous char's
     * background?
     * // Yes.
     * // So we need to identify BACKGROUND.
     * // If Python script converts to RGB, transparent becomes Black (0,0,0)
     * usually? Or White?
     * // If it was "VB_Alphabet_ENG.png", it likely has transparent background.
     * // PIL convert("RGB") on transparent image:
     * // If it has alpha, it might become black or white depending on PIL
     * version/settings.
     * // The script defines white=(255,255,255).
     * // It checks `!= white`.
     * // So White is Background. Text is Non-White.
     * 
     * // My logic:
     * // isText(color) -> returns true if it is text.
     * // if (isText(firstColColor)) { y++; continue; }
     * 
     * // So I need to correctly identify Text vs Background.
     * // I will assume Background is Transparent (Alpha=0) OR White (1,1,1).
     */

    private boolean isBackground(Color color) {
        return color.getOpacity() == 0 || color.equals(Color.WHITE) || color.equals(Color.TRANSPARENT);
    }

    private boolean isVisible(Color color) {
        if (color.getOpacity() == 0)
            return false; // Transparent is always background

        // If we have a detected background color, use it
        if (currentBackgroundColor != null) {
            return !isSameColor(color, currentBackgroundColor);
        }
        // Fallback to old logic (assuming transparent background)
        return color.getOpacity() > 0.1 && (color.getRed() > 0.1 || color.getGreen() > 0.1 || color.getBlue() > 0.1);
    }

    private boolean isSameColor(Color c1, Color c2) {
        if (c1.getOpacity() == 0 && c2.getOpacity() == 0)
            return true; // Both transparent
        if (Math.abs(c1.getOpacity() - c2.getOpacity()) > 0.1)
            return false;

        double diff = Math.abs(c1.getRed() - c2.getRed()) +
                Math.abs(c1.getGreen() - c2.getGreen()) +
                Math.abs(c1.getBlue() - c2.getBlue());
        return diff < 0.1; // Tolerance
    }

    private boolean isVisible(int rgb) {
        // Check if not green background
        int green = 0x00FF00;
        return (rgb & 0xFFFFFF) != green;
    }

    private boolean checkCollision(BufferedImage canvas, int x, int y) {
        if (x < 0 || x >= canvas.getWidth() || y < 0 || y >= canvas.getHeight())
            return false;
        int[] dy = { 0, -1, -2, 1, 2 };
        for (int d : dy) {
            int checkY = y + d;
            if (checkY >= 0 && checkY < canvas.getHeight()) {
                if (isVisible(canvas.getRGB(x, checkY)))
                    return true;
            }
        }
        return false;
    }

    private void fillGreen(BufferedImage image) {
        int green = 0x00FF00;
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                image.setRGB(i, j, green);
            }
        }
    }

    private void pasteImage(BufferedImage canvas, Image charImg, int x, int y) {
        PixelReader reader = charImg.getPixelReader();
        for (int i = 0; i < charImg.getWidth(); i++) {
            for (int j = 0; j < charImg.getHeight(); j++) {
                Color c = reader.getColor(i, j);
                if (c.getOpacity() > 0) {
                    int rgb = (int) (c.getRed() * 255) << 16 | (int) (c.getGreen() * 255) << 8
                            | (int) (c.getBlue() * 255);
                    canvas.setRGB(x + i, y + j, rgb);
                }
            }
        }
    }

    private int calculateTextWidth(BufferedImage canvas) {
        int width = canvas.getWidth();
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < canvas.getHeight(); y++) {
                if (isVisible(canvas.getRGB(x, y))) {
                    return x + 1;
                }
            }
        }
        return 0;
    }

    private int calculateMargin(int canvasWidth, int textWidth, String name) {
        int lmargin = 0;
        boolean isSerif = "AMSVW".indexOf(name.charAt(0)) >= 0;

        if (canvasWidth == 80) {
            int x = (80 - textWidth) / 80; // This logic in python seems weird: x = (80 - text_w) // 80. If text_w < 80,
                                           // x is 0.
            // Wait, python: x = (80 - text_w) // 80. If text_w is 70, x=0. lmargin = -2.
            // If text_w is 10, x=0.
            // Maybe I should just copy the logic exactly.
            // Python: x = (80 - text_w) // 80
            // lmargin = x - 2
            // This seems to almost always result in -2?
            // Ah, wait. If text_w is small, say 10. 80-10 = 70. 70//80 = 0.
            // Unless text_w is very small? No.
            // Maybe it meant % 80? Or just centering?
            // Let's look at the python code again.
            // x = (80 - text_w) // 80
            // lmargin = x - 2
            // This looks like it might be a bug in the original script or I am
            // misinterpreting.
            // But I must follow the script.

            // Actually, let's look at the centering logic in general.
            // Usually we want (CanvasWidth - TextWidth) / 2.
            // The script logic seems specific.

            // Let's implement a standard centering if the script logic seems broken,
            // OR stick to the script if I want 1:1 port.
            // Given "Fork : SwayStation", maybe it's specific.
            // But (80 - text_w) // 80 is 0 for any text_w in (1, 80).
            // So lmargin becomes -2.
            // Then `if lmargin < 0: lmargin = 0`.
            // So lmargin is always 0 for 80px?

            lmargin = 0;
        } else if (canvasWidth == 160) {
            int x = isSerif ? 3 : 4;
            if (textWidth + x > 160) {
                lmargin = 0;
            } else {
                lmargin = 0; // The python script sets lmargin = 0 in BOTH branches?
            }
        } else {
            // Same for > 160
            lmargin = 0;
        }

        // The python script seems to have logic that effectively results in 0 margin
        // most of the time?
        // "if text_w + x > 240: lmargin = 0 else: lmargin = 0"
        // It seems the margin logic in the python script might be vestigial or broken.
        // I will stick to 0 for now.
        return lmargin;
    }

    private SpriteData.Sprite convertToSprite(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixelData = new byte[width * height * 2];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Convert to R5G6B5
                int r = (red >> 3) & 0x1F;
                int g = (green >> 2) & 0x3F;
                int b = (blue >> 3) & 0x1F;

                // Pack: RRRRRGGG GGGBBBBB
                // Byte 0: RRRRRGGG (top 3 bits of G)
                // Byte 1: GGGBBBBB (bottom 3 bits of G)
                // Wait, Little Endian?
                // SpriteImageTranslator:
                // byte0 = (byte) (((red & 0xFF) << 3) | ((green & 0xFF) >> 3));
                // byte1 = (byte) (((green & 0xFF) << 5) | (blue & 0xFF));
                // bytes[index] = byte1;
                // bytes[index + 1] = byte0;

                // Let's use the same logic
                int r5 = (int) Math.floor(red / 255.0 * 31.0);
                int g6 = (int) Math.floor(green / 255.0 * 63.0);
                int b5 = (int) Math.floor(blue / 255.0 * 31.0);

                byte byte0 = (byte) ((r5 << 3) | (g6 >> 3));
                byte byte1 = (byte) ((g6 << 5) | b5);

                int index = (y * width + x) * 2;
                pixelData[index] = byte1;
                pixelData[index + 1] = byte0;
            }
        }

        return SpriteData.Sprite.builder()
                .width(width)
                .height(height)
                .pixelData(pixelData)
                .build();
    }
}
