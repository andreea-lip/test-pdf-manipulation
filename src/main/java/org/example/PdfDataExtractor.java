package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfDataExtractor {

    private final String pathToDocument;

    /**
     * Find the document at the given path
     * */
    public PdfDataExtractor(String pathToDocument) {
        this.pathToDocument = pathToDocument;
    }

    /**
     * Function checks if a pixel is orange-ish
     * @param rgb Colors can be stored as an int in this format -> 0xAARRGGBB
     * @return true if the pixel passes what is deemed as the orange threshold
    */
    private boolean isOrange(int rgb) {
        // shift the bits and use a mask to get the needed value
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return (red > 200 && green > 150 && blue < 120);
    }

    /**
     * Function checks if a pixel is coloured (not white)
     * @param rgb Colors can be stored as an int in this format -> 0xAARRGGBB
     * @return true if the pixel passes what is deemed as the coloured threshold
     */
    private boolean isColoured(int rgb) {
        // shift the bits and use a mask to get the needed value
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // compute distance from pure white (255, 255, 255)
        int diffRed = 255 - red;
        int diffGreen = 255 - green;
        int diffBlue = 255 - blue;

        // if all values are very close to 255, it's white
        // otherwise treat as "colored"
        return (diffRed > 23 || diffGreen > 23 || diffBlue > 23);
    }

    /**
     * Function identifies the rows of the diagram.
     * It calculates the sum of all the pixels in a row of the image. If the sum of pixels is bigger than a certain threshold,
     * it means the row of pixels contains pixels from the diagram (therefore it is NOT an empty white row, which we ignore)
     * @param img The image containing the diagram
     * @return A list of coordinates for each row
     * */
    private List<int[]> detectRowBands(BufferedImage img) {
        int height = img.getHeight();
        int width = img.getWidth();
        int[] rowSums = new int[height];

        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {
                if (isColoured(img.getRGB(x, y))) count++;
            }
            rowSums[y] = count;
        }

        List<int[]> bands = new ArrayList<>();
        boolean inBand = false;
        int bandStart = 0;

        for (int y = 0; y < height; y++) {
            if (!inBand && rowSums[y] > 10) { // threshold to enter band
                inBand = true;
                bandStart = y;
            } else if (inBand && rowSums[y] <= 10) { // threshold to exit band
                inBand = false;
                bands.add(new int[]{bandStart, y});
            }
        }

        return bands;
    }

    /**
     * Analog ^
     * */
    private List<int[]> detectColumnBands(BufferedImage img) {
        int height = img.getHeight();
        int width = img.getWidth();
        int[] colSums = new int[width];

        for (int x = 0; x < width; x++) {
            int count = 0;
            for (int y = 0; y < height; y++) {
                if (isColoured(img.getRGB(x, y))) count++;
            }
            colSums[x] = count;
        }

        List<int[]> bands = new ArrayList<>();
        boolean inBand = false;
        int bandStart = 0;

        for (int x = 0; x < width; x++) {
            if (!inBand && colSums[x] > 25) { // threshold to enter band
                inBand = true;
                bandStart = x;
            } else if ((inBand && colSums[x] <= 25) || (inBand && x == width - 1) ) { // threshold to exit band
                inBand = false;
                bands.add(new int[]{bandStart, x});
            }
        }

        return bands;
    }

    /**
     * Counts the amount of orange pixels inside a given rectangle
     * */
    private int countOrangePixels(BufferedImage img, int yStart, int yEnd, int xStart, int xEnd) {
        int orangeCount = 0;

        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                if (isOrange(img.getRGB(x, y))) {
                    orangeCount++;
                }
            }
        }

        return orangeCount;
    }

    /**
     * Builds the result matrix where for one orange square we have a 1 and for grey squares we have 0
     * */
    private int[][] extractMatrix(BufferedImage img) {
        List<int[]> rowBands = detectRowBands(img);
        List<int[]> colBands = detectColumnBands(img);

        int rows = rowBands.size();
        int cols = colBands.size();
        int[][] matrix = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            // get coordinates for start & end of column
            int yStart = rowBands.get(r)[0];
            int yEnd = rowBands.get(r)[1];

            for (int c = 0; c < cols; c++) {
                // get coordinates for start & end of row
                int xStart = colBands.get(c)[0];
                int xEnd = colBands.get(c)[1];

                // count how many orange pixels in the rectangle
                int orangeCount = countOrangePixels(img, yStart, yEnd, xStart, xEnd);
                int totalPixels = (xEnd - xStart) * (yEnd - yStart);

                // if the orange count meets the threshold, then the rectangle is orange
                double ratio = (double) orangeCount / totalPixels;
                matrix[r][c] = (ratio > 0.3) ? 1 : 0; // 30% threshold
            }
        }

        return matrix;
    }

    /**
     * Crops an image form a pdf document. Right now it crops exactly around the orange diagram.
     * For future, it should crop at some given coordinates so we can use it to crop the other 2 diags as well.
     * */
    private BufferedImage getCropped(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage fullPage = renderer.renderImageWithDPI(1, 300);

        // Crop diagram area (coordinates in pixels)
        int startX = 550;
        int startY = 590;
        int width = 500;
        int height = 2000;
        // todo questions: is there any variation in the pdfs? in the size of the diagram, is the number of pages?

        return fullPage.getSubimage(startX, startY, width, height);
    }

    /**
     * Removes the dark pixels from around the diagram, aka the text.
     * */
    private BufferedImage removeDarkPixels(BufferedImage image) {
        // todo do the pixel shades stay the same? if they change this code might need changing

        // Threshold for dark pixels (0-255)
        int darkThreshold = 230;

        // Make dark pixels white
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int rgb = image.getRGB(i, j);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (r < darkThreshold && g < darkThreshold && b < darkThreshold) {
                    // Set pixel to white
                    int white = (255 << 16) | (255 << 8) | 255;
                    image.setRGB(i, j, white);
                }
            }
        }

        return image;
    }

    /**
     * Main function of the class
     * */
    public int[][] extractData() throws Exception {

        String absolutePath = "C:/Users/andreea.lipan/OneDrive - ACCESA/Projects/test-pdf-manipulation";

        // Find the document at the specified path
        File file = new File(pathToDocument);
        PDDocument document = PDDocument.load(file);

        // Crop the orange diagram out of it
        BufferedImage cropped = getCropped(document);

        // Save the modified image - just for testing purposes
        ImageIO.write(cropped, "png", new File(absolutePath + "/src/main/resources/output/diagram1.png")); // requires full file path dunno why

        // Remove the text around the cropped image
        removeDarkPixels(cropped);

        // Save the modified image - just for testing purposes
        ImageIO.write(cropped, "png", new File(absolutePath + "/src/main/resources/output/diagram2.png")); // requires full file path dunno why

        document.close();

        // Extract matrix
        return extractMatrix(cropped);
    }
}

