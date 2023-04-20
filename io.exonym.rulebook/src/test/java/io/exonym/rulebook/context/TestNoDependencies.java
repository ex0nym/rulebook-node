package io.exonym.rulebook.context;

import io.exonym.abc.util.UidType;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.WhiteList;
import io.exonym.utils.RulebookVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

public class TestNoDependencies {

    private static final Logger logger = LogManager.getLogger(TestNoDependencies.class);

    @Test
    void rulebookVerification() throws Exception {
        RulebookVerifier verifier = new RulebookVerifier(new URL("https://trust.exonym.io/sybil-rulebook.json"));
        Rulebook r = verifier.getRulebook();
        logger.debug(r.getRulebookId());

    }

    @Test
    void whitelist() {
        assert (WhiteList.isNumbers("12345467890"));
        assert (!WhiteList.isNumbers("371f6ee2b97641cc8bf9e724408ed6c3"));
        assert (WhiteList.containsNumbers("12345467890"));
        assert (WhiteList.containsNumbers("1234546d890"));
        // todo

    }

    @Test
    void UidHeler() {
        try {
            URI uid = URI.create("urn:rulebook:7a13071495188f94e6bc1432f90981160ce730d7d7cd01f3f539d7e4f0e55afa");

            boolean f = UidType.isRulebook(uid);
            logger.debug(f);

//            URI uid0 = URI.create(Namespace.URN_PREFIX_COLON + "source-name:advocate-name:0123456789abcdef:1234:i");
//            UIDHelper helper = new UIDHelper(uid0);
//            helper.out();


        } catch (Exception e) {
            logger.debug("e", e);
            assert false;

        }
    }
}
