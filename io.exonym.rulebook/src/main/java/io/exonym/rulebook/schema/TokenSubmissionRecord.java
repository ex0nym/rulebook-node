package io.exonym.rulebook.schema;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

public class TokenSubmissionRecord extends AbstractCouchDbObject {

    private long lastCompletedIndex = 0;
    private String lastIndexName;

    public long getLastCompletedIndex() {
        return lastCompletedIndex;
    }

    public void setLastCompletedIndex(long lastCompletedIndex) {
        this.lastCompletedIndex = lastCompletedIndex;
    }

    public String getLastIndexName() {
        return lastIndexName;
    }

    public void setLastIndexName(String lastIndexName) {
        this.lastIndexName = lastIndexName;
    }
}
