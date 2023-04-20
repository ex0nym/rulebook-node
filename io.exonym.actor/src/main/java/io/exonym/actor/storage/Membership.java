package io.exonym.actor.storage;

import io.exonym.lite.pojo.Namespace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;

@XmlRootElement(name="Membership", namespace = Namespace.EX)
@XmlType(name="Membership", namespace = Namespace.EX)
public class Membership {

    private ArrayList<MembershipToken> tokens = new ArrayList<MembershipToken>();

    @XmlElement(name = "MembershipToken", namespace = Namespace.EX)
    public ArrayList<MembershipToken> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<MembershipToken> tokens) {
        this.tokens = tokens;
    }
}
