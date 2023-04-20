package io.exonym.lite.pojo;

import java.util.ArrayList;

public class Prove {

    private ArrayList<String> blacklist = new ArrayList<>();
    private String sourceUuid;

    public ArrayList<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(ArrayList<String> blacklist) {
        this.blacklist = blacklist;

    }

    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }
}
