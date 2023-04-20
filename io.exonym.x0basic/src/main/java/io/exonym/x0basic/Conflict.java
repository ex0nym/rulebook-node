package io.exonym.x0basic;

import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;

import java.util.List;

public class Conflict implements Msg {

    private List<ExoMatrix> matrices;
    private ExoNotify notify;

    public List<ExoMatrix> getMatrices() {
        return matrices;
    }

    public void setMatrices(List<ExoMatrix> matrices) {
        this.matrices = matrices;
    }

    public ExoNotify getNotify() {
        return notify;
    }

    public void setNotify(ExoNotify notify) {
        this.notify = notify;
    }
}
