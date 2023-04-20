package io.exonym.lite.standard;


import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipInMemoryManager implements Closeable {


    private ZipOutputStream zos;
    private ByteArrayOutputStream bos;

    private final String fileName;

    public ZipInMemoryManager(String fileName) {
        this.fileName = fileName;

    }

    public void newZipFile() {
        bos = new ByteArrayOutputStream();
        zos = new ZipOutputStream(bos);

    }

    /**
     *
     * @param fileName
     * @param fileContent
     * @throws java.util.zip.ZipException If an entry with the same name has already been assigned
     * @throws IOException
     */
    public void addFileToZip(String fileName, byte[] fileContent) throws IOException {
        ZipEntry item = new ZipEntry(fileName);
        zos.putNextEntry(item);
        zos.write(fileContent);
        zos.flush();

    }

    @Override
    public void close() throws IOException {
        bos.close();
        zos.close();

    }

    public byte[] getBytes() throws IOException {
        zos.flush();
        zos.close();
        bos.flush();
        byte[] buf = bos.toByteArray();
        bos.close();
        return buf;

    }

    public void writeToFile() throws IOException {
        bos.flush();
        bos.close();


    }

    public String getFileName() {
        return fileName;
    }
}
