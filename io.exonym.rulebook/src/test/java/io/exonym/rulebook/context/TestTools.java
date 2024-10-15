package io.exonym.rulebook.context;

import java.net.URI;
import java.nio.file.Path;
import java.util.logging.Logger;

public class TestTools {

    private final static Logger logger = Logger.getLogger(TestTools.class.getName());
    public static final String PASSWORD = "password";
    public static final Path STORE_FOLDER = Path.of("non-resources");

    public static final Path TOKENS = Path.of("resources")
            .toAbsolutePath()
            .getParent()
            .getParent()
            .resolve(Path.of("io.exonym.example.sso", "tokens"));

    public static final int SYBIL_PORT = 8079;
    public static final int SYBIL_RB_PORT = 8080;
    public static final int NODE_0_PORT = 8081;
    public static final int NODE_1_PORT = 8082;

    public static final String[] SYBIL_RB_API = {
            "0d7104b8-77ed-4546-b625-04572974573b",
            "8027e77400b7ce7146449b06524040e16447754077c049b907d9715cc48cf417"
    };
    public static final URI RULEBOOK_UID = URI.create("urn:rulebook:trustworthy-leaders:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    public static final URI LEAD_UID = URI.create("urn:rulebook:trustworthy-leaders:exonym:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    public static final URI MOD0_UID = URI.create("urn:rulebook:trustworthy-leaders:exonym:exonym-leads:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    public static final URI MOD1_UID = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");

    public static final String[] NODE_0_API = {
            "a32c8791-dbea-42e8-b928-4a88ba1de3ea",
            "f2335e185a334d9d620070df592f802369e0fc0bf73e4f4316285dd847d09d51"
    };

    public static final String[] NODE_1_API = {
            "e357838c-a8c2-4c1e-9e49-a641bd8e23b0",
            "e70fea3df7af08d7f5f3a5e815797c7d9951c3d777edeb218ae8d975ae931f07"
    };


}


