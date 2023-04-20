package io.exonym.actor.storage;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class PublisherConfig {
    
    private static final Logger logger = LogManager.getLogger(PublisherConfig.class);


    private String jsLocation = "js/exonym-paywall.js";
    private String prefix = "prePayWall";
    private URL paywallBanner;
    private String cssLocation = "css/exonym-paywall.css";
    private SFTPLogonData publishTo;
    private final File outputPath;

    public PublisherConfig(File outputPath) {
        this.publishTo = null;
        this.outputPath = outputPath;
        try {
            this.paywallBanner = new URL("https://exonym.io/resource/PayWallBanner.html");

        } catch (MalformedURLException e) {
            logger.error("Error", e);

        }
    }

    public PublisherConfig(SFTPLogonData publishTo) {
        this.publishTo = publishTo;
        this.outputPath = null;
        try {
            this.paywallBanner = new URL("https://exonym.io/resource/PayWallBanner.html");

        } catch (MalformedURLException e) {
            logger.error("Error", e);

        }

    }

}
