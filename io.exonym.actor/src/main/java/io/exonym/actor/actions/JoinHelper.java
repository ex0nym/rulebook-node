package io.exonym.actor.actions;

import eu.abc4trust.smartcard.Base64;
import eu.abc4trust.xml.*;
import io.exonym.helpers.BuildPresentationPolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.pojo.NetworkMapItemSource;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.ExternalResourceContainer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JoinHelper {
    public static final URI BASE_ALIAS = URI.create("urn:io:exonym");
    public static final URI SYBIL_ALIAS = URI.create("urn:io:exonym:sybil");

    public static PresentationPolicy baseJoinPolicy(RulebookVerifier verifier,
                                                                URI sybilIssuer,
                                                                ExternalResourceContainer ex,
//                                                                AbstractNetworkMap networkMap,
//                                                                ArrayList<NetworkMapItemSource> acceptableSources,
                                                                String challengeB64) throws Exception {
        BuildPresentationPolicy bppPlain = generateBaseAlternative(verifier, sybilIssuer, ex, challengeB64);
        return bppPlain.getPolicy();
        // todo the user can upgrade or downgrade their rulebook credential - the exonym x and y lists are done, but the challenges aren't

//        BuildPresentationPolicy bppAlt = generateBaseAlternative(verifier, sybilIssuer, ex, challengeB64);
//        filterSourceList(acceptableSources, verifier);
//        List<IssuerParameters> possibleParams = new ArrayList<>();
//        for (NetworkMapItemSource source : acceptableSources){
//
//            possibleParams.add(UIDHelper.computeIssuerParametersUID(source.))
//
//
//        }
//        bppAlt.addCredentialInPolicy(null, null, null, null);
//        return null;

    }

    private static void filterSourceList(ArrayList<NetworkMapItemSource> acceptableSources, RulebookVerifier verifier) {
        String rid = UIDHelper.computeRulebookHashFromRulebookId(verifier.getRulebook().getRulebookId());
        ArrayList<NetworkMapItemSource> result = new ArrayList<>();
        for (NetworkMapItemSource source : acceptableSources){
            if (source.getSourceUID().toString().contains(rid)){
                result.add(source);
            }
        }
        acceptableSources.clear();
        acceptableSources.addAll(result);

    }

    private static BuildPresentationPolicy generateBaseAlternative(RulebookVerifier verifier,
                                                                   URI sybilIssuer,
                                                                   ExternalResourceContainer ex,
                                                                   String challengeB64) throws Exception {
        BuildPresentationPolicy bpp = new BuildPresentationPolicy(
                URI.create("urn:io:exonym:join"), ex);
        bpp.addPseudonym(SYBIL_ALIAS.toString(), true, BASE_ALIAS.toString());
        ArrayList<String> rules = verifier.toRulebookUIDs();
        for (String rule : rules){
            bpp.addPseudonym(rule, true, null, BASE_ALIAS);

        }
        bpp.makeInteractive(challengeB64);
        UIDHelper helper = new UIDHelper(sybilIssuer);
        List<URI> credSpecs = new ArrayList<>();
        credSpecs.add(helper.getCredentialSpec());
        List<CredentialInPolicy.IssuerAlternatives.IssuerParametersUID> issuerParametersUIDS = new ArrayList<>();
        issuerParametersUIDS.add(helper.computeIssuerParametersUID());
        bpp.addCredentialInPolicy(credSpecs, issuerParametersUIDS, SYBIL_ALIAS.toString(), BASE_ALIAS);
        return bpp;

    }

    public static PresentationPolicy baseJoinPolicy(RulebookVerifier verifier,
                                                                URI sybilIssuer,
                                                                ExternalResourceContainer ex,
                                                                byte[] nonceFromMessage) throws Exception {
        return baseJoinPolicy(verifier, sybilIssuer, ex, Base64.encodeBytes(nonceFromMessage));
    }

}
