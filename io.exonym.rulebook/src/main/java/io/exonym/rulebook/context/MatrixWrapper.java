package io.exonym.rulebook.context;

import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;

public class MatrixWrapper implements Msg {

    private final ExoMatrix matrix;

    public MatrixWrapper(ExoMatrix matrix) {
        this.matrix = matrix;
    }

    public ExoMatrix getMatrix() {
        return matrix;
    }
}
