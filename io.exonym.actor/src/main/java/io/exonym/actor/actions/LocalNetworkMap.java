package io.exonym.actor.actions;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.NetworkMapItemAdvocate;
import io.exonym.lite.pojo.NetworkMapItemSource;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class LocalNetworkMap extends AbstractNetworkMap {

    public LocalNetworkMap() throws Exception {
    }

    @Override
    protected Path defineRootPath() {
        return Path.of("local", "network-map");
    }

    @Override
    protected Cache instantiateCache() throws Exception {
        return new Cache();
    }

    @Override
    protected void writeVerifiedSource(String rulebookId, String source, NetworkMapItemSource nmis,
                                       ArrayList<NetworkMapItemAdvocate> advocatesForSource) throws Exception {

        Path pathSource = pathToSourcePath(rulebookId, source);
        Files.createDirectories(pathSource);
        Path pathSourceNMI = pathToRootPath()
                .resolve(rulebookId)
                .resolve(toNmiFilename(nmis.getSourceUID()));

        try (BufferedWriter bw = Files.newBufferedWriter(pathSourceNMI)) {
            bw.write(JaxbHelper.serializeToJson(nmis, NetworkMapItemSource.class));
            bw.flush();

        } catch (Exception e) {
            throw e;

        }
        for (NetworkMapItemAdvocate advocate : advocatesForSource){
            String advocateFileName = toNmiFilename(advocate.getNodeUID());
            Path path = pathSource.resolve(advocateFileName);
            try (BufferedWriter bw = Files.newBufferedWriter(path)) {
                bw.write(JaxbHelper.serializeToJson(advocate, NetworkMapItemAdvocate.class));
                bw.flush();

            } catch (Exception e) {
                throw e;

            }
        }
    }

    @Override
    public boolean networkMapExists() throws Exception {
        return Files.exists(pathToRootPath());

    }

    @Override
    protected NodeVerifier openNodeVerifier(URL staticNodeUrl0, URL staticNodeUrl1, boolean isTargetSource) throws Exception {
        return NodeVerifier.tryNode(staticNodeUrl0, staticNodeUrl0, true, false);
    }
}
