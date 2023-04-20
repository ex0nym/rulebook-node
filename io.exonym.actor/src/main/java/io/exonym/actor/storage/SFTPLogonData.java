package io.exonym.actor.storage;

import java.net.URI;

public class SFTPLogonData {

    private URI sftpUID;
    private  int port;
    private  String host;

    private String username;
    private String password;

    private String kn0;
    private String kn1;
    private String kn2;

    public SFTPLogonData() {
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsernameAndPassword(String username, String password) {
        if (username==null || password==null){
            throw new NullPointerException();

        }
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getKn0() {
        return kn0;
    }

    public String getKn1() {
        return kn1;
    }

    public String getKn2() {
        return kn2;
    }

    public void setKnownHosts(String k0, String k1, String k2) {
        if (k0==null || k1==null || k2==null){
            throw new NullPointerException();

        }
        this.kn0 = k0;
        this.kn1 = k1;
        this.kn2 = k2;

    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return this.username + " " + this.password + " \\n\\t " + this.kn0;
    }
}
