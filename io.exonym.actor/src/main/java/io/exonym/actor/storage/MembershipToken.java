package io.exonym.actor.storage;

import io.exonym.lite.pojo.Namespace;

import javax.xml.bind.annotation.XmlElement;
import java.net.URI;

public class MembershipToken {

    private String fileName;
    private URI sourceUid;
    private URI nodeUid;

    @XmlElement(name = "FileName", namespace = Namespace.EX)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @XmlElement(name = "SourceUID", namespace = Namespace.EX)
    public URI getSourceUid() {
        return sourceUid;
    }

    public void setSourceUid(URI sourceUid) {
        this.sourceUid = sourceUid;
    }

    @XmlElement(name = "NodeUID", namespace = Namespace.EX)
    public URI getNodeUid() {
        return nodeUid;
    }

    public void setNodeUid(URI nodeUid) {
        this.nodeUid = nodeUid;
    }
}
