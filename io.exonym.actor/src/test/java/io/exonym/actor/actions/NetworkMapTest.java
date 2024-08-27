package io.exonym.actor.actions;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.NetworkMapItemModerator;
import io.exonym.lite.pojo.NetworkMapItemLead;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class NetworkMapTest extends AbstractNetworkMap {

    private NetworkMapItemModerator nmiForMyModerator;
    private NetworkMapItemLead nmiForMyLead;


    public NetworkMapTest(NetworkMapItemLead nmiForMyLead, NetworkMapItemModerator nmiForMyModerator) throws Exception {
        this.nmiForMyModerator = nmiForMyModerator;
        this.nmiForMyLead = nmiForMyLead;
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
    protected void writeVerifiedSource(String rulebookId, String source, NetworkMapItemLead nmis,
                                       ArrayList<NetworkMapItemModerator> advocatesForSource) throws Exception {

        Path pathSource = pathToSourcePath(rulebookId, source);
        Files.createDirectories(pathSource);
        Path pathSourceNMI = pathToRootPath()
                .resolve(rulebookId)
                .resolve(toNmiFilename(nmis.getLeadUID()));

        try (BufferedWriter bw = Files.newBufferedWriter(pathSourceNMI)) {
            bw.write(JaxbHelper.serializeToJson(nmis, NetworkMapItemLead.class));
            bw.flush();

        } catch (Exception e) {
            throw e;

        }
        for (NetworkMapItemModerator advocate : advocatesForSource){
            String advocateFileName = toNmiFilename(advocate.getNodeUID());
            Path path = pathSource.resolve(advocateFileName);
            try (BufferedWriter bw = Files.newBufferedWriter(path)) {
                bw.write(JaxbHelper.serializeToJson(advocate, NetworkMapItemModerator.class));
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
    protected NodeVerifier openNodeVerifier(URI staticNodeUrl0, URI staticNodeUrl1, boolean isTargetSource) throws Exception {
        return NodeVerifier.tryNode(staticNodeUrl0, staticNodeUrl0, true, false);
    }




    @Override
    public NetworkMapItemLead nmiForMyNodesSource() throws Exception {
        return nmiForMyLead;
    }

    @Override
    public NetworkMapItemModerator nmiForMyNodesAdvocate() throws Exception {
        return nmiForMyModerator;

    }
}
