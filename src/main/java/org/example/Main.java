package org.example;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    // takes images from a pdf
    // pdf-ul 2 e facut de mine si are o imagine in el, just for testing purposes
    // pune imaginile in folderul output
    public static void pbfboximages() {

        // aici a avut nevoie de path-uri absolute, nu am reusit sa fac sa mearga altfel, idk why
        String pdfPath = "/src/main/resources/test-pdf-1.pdf";
        String outputDir = "/src/main/resources/output";

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                System.out.println("Page");
                PDResources resources = page.getResources();
                for (COSName xObjectName : resources.getXObjectNames()) {
                    if (resources.isImageXObject(xObjectName)) {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(xObjectName);
                        BufferedImage bImage = image.getImage();

                        File outputFile = new File(outputDir, "image_" + pageNum + "_" + xObjectName.getName() + ".png");
                        ImageIO.write(bImage, "png", outputFile);
                    }
                }
                pageNum++;
            }
            System.out.println("Images extracted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // tries to crop the diagrams from the pdf
    // also needs absolute path to work (for now at least :) )
    // imaginea rezultata va fi in /output/diagram.png
    public static void pdfboximagecrop() throws Exception {
        File file = new File("/src/main/resources/test-pdf-3.pdf");
        PDDocument document = PDDocument.load(file);

        PDPage page = document.getPage(1);

        // Define the crop box (in PDF units, origin = bottom-left)
        float x = 152;   // startX
        float y = 200;   // startY
        float width = 54;
        float height = 427;
        PDRectangle rect = new PDRectangle(x, y, width, height);

        // Temporarily apply crop
        page.setCropBox(rect);

        // Render only that region
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage diagramImg = renderer.renderImageWithDPI(1, 300);

        ImageIO.write(diagramImg, "png", new File("/src/main/resources/output/diagram.png"));

        document.close();
    }

    // take only the text from the pdf
    public static void getTextFromPdf() {
        File pdfFile = new File("src/main/resources/test-pdf-1.pdf");

        try (PDDocument doc = PDDocument.load(pdfFile)) {

            if (!doc.isEncrypted()) {
                PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
                textStripper.setSortByPosition(true);

                PDFTextStripper textStripper1 = new PDFTextStripper();
                String pdfFileinText = textStripper1.getText(doc);
                //System.out.println("Text: " + pdfFileinText);

                Arrays.stream(pdfFileinText.split("\\r?\\n")).forEach(System.out::println);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // prog start
    public static void main(String[] args) throws Exception {

        pdfboximagecrop();

    }
}
