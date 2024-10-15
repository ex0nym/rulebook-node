package io.exonym.lite.exceptions;

import io.exonym.lite.pojo.ApplicantReport;

public class PenaltyException extends UxException {

    private ApplicantReport report;

    public PenaltyException(String msg) {
        super(msg);
    }

    public PenaltyException(String msg, String... required) {
        super(msg, required);
    }

    public PenaltyException(String msg, Throwable e, String... required) {
        super(msg, e, required);
    }

    public PenaltyException(String msg, Throwable e) {
        super(msg, e);
    }

    public ApplicantReport getReport() {
        return report;
    }

    public void setReport(ApplicantReport report) {
        this.report = report;
    }
}
