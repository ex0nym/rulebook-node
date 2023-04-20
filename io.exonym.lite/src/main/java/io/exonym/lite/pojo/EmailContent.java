package io.exonym.lite.pojo;

import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.standard.WhiteList;

public class EmailContent {

    public static final String TYPE_HTML = "text/html";
    public static final String TYPE_PLAIN = "text/plain";

    private String from;
    private String fromName;
    private String to;
    private String subject;
    private String contentType;
    private String content;

    public EmailContent(String from, String to, String subject, String contentType, String content) throws HubException {
        if (!WhiteList.email(from) && !WhiteList.email(to)
                && subject!=null && contentType!=null && content!=null) {
            throw new HubException("from: " + from + " " + WhiteList.email(from) + " " +
                    "\n\t\tto: " + to + " " + WhiteList.email(to) + " " +
                    "\n\t\tsubject: " + subject +
                    "\n\t\tcontentType: " + contentType +
                    "\n\t\tcontent: " + content);

        }
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.contentType = contentType;
        this.content = content;

    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
