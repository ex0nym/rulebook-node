package io.exonym.lite.standard;


import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;

public class DirectoryWithFiles implements FilenameFilter {


    private final HashSet<String> types = new HashSet<>();
    private File[] files;

    /**
     *
     * @param directory the parent directory where you want to search
     * @param fileTypes list the file types including the dot, so ".html", ".htm"
     * @throws Exception
     */
    public DirectoryWithFiles(File directory, String... fileTypes) throws Exception {
        if (!directory.isDirectory()){
            throw new Exception(directory  + " is not a directory.");

        }
        for (String f : fileTypes){
            types.add(f.toLowerCase());

        }
        files = directory.listFiles(this);

    }

    @Override
    public boolean accept(File dir, String name) {
        name = name.toLowerCase();
        String[] s = name.split("\\.");
        String suffix = "." + s[s.length-1];
        return types.contains(suffix);

    }

    public File[] getFiles() {
        return files;
    }
}
