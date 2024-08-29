package io.exonym.rulebook.context;

import io.exonym.lite.parallel.Msg;

public class BroadcastStringIn implements Msg {

    private String value;

    public BroadcastStringIn(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
