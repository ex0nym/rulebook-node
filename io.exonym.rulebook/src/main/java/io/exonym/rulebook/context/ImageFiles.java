package io.exonym.rulebook.context;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

public class ImageFiles implements FilenameFilter {
    
    private static final Logger logger = LogManager.getLogger(ImageFiles.class);

    private File[] images;
    private HashMap<String, File> files = new HashMap<>();

    public ImageFiles(String directory) {
        File f = new File(directory);
        images = f.listFiles(this);
        resolve();

    }

    private void resolve() {
        for (File i : images){
            files.put(i.getName(), i);

        }
    }

    public ImageFiles(File directory) {
        images = directory.listFiles(this);
        resolve();

    }

    @Override
    public boolean accept(File dir, String name) {
        name = name.toLowerCase();
        return name.endsWith(".jpg") ||
                name.endsWith(".pgm") ||
                name.endsWith(".png");

    }

    public File[] getImages() {
        return images;

    }

    public File getImage(String name){
        return files.get(name);

    }

    public static BufferedImage toBufferedImage(String b64Image) throws IOException {
        byte[] imageBytes = Base64.decodeBase64(b64Image);
        return toBufferedImage(imageBytes);

    }

    public static BufferedImage toBufferedImage(byte[] image) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(image);
        return ImageIO.read(bis);

    }

    public static BufferedImage downsizeWidth(BufferedImage image, int width) throws IOException {
        int h =  image.getHeight();
        int w = image.getWidth();
        float scale = (float)width/(float)w;
        logger.debug("Image Dimensions before resize " + w + " " + h);
        if (scale>1){
            logger.warn("cannot scale up with this method - no scaling applied.  Width is: " + w);
            return image;

        } if (scale==0){
            return image;

        }
        logger.debug("Applied scaling of " + scale);
        h = (int) (h * scale);
        return resize(image, width, h);

    }

    public static BufferedImage downsizeHeight(BufferedImage image, int height) throws IOException {

        int h =  image.getHeight();
        int w = image.getWidth();
        float scale = (float)height/(float)h;
        if (scale>1){
            logger.warn("cannot scale up with this method - no scaling applied.  Width is: " + w);
            return image;

        }
        logger.debug("Applied scaling of " + scale);
        w = (int) (w * scale);
        return resize(image, w, height);

    }

    public static BufferedImage resize(BufferedImage image, int w, int h) throws IOException {
        return Thumbnails.of(image).forceSize(w, h).asBufferedImage();

    }

    public static String getImageAsBase64(File f) throws Exception {
        BufferedImage i = ImageIO.read(f);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String[] split = f.getName().split("\\.");
        if (split.length==0){
            throw new Exception("No filename suffix found for name: " + f.getName());

        }
        ImageIO.write(i, split[split.length-1], bos);
        return Base64.encodeBase64String(bos.toByteArray());

    }

    public static byte[] compress(File imageFile) throws IOException{
        InputStream inputStream = new FileInputStream(imageFile);
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        inputStream.close();
        return compress(bufferedImage);

    }

    public static byte[] compress(BufferedImage bufferedImage) throws IOException {
        float imageQuality = 0.3f;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpg");
        if (!imageWriters.hasNext()) {
            throw new IllegalStateException("Writer not found");

        }
        ImageWriter imageWriter = imageWriters.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(bos);
        imageWriter.setOutput(imageOutputStream);

        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(imageQuality);

        imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);

        imageOutputStream.flush();
        bos.flush();
        bos.close();
        imageOutputStream.close();
        imageWriter.dispose();

        return bos.toByteArray();

    }
}
