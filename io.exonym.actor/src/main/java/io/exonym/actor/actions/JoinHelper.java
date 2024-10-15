package io.exonym.actor.actions;

import eu.abc4trust.smartcard.Base64;
import eu.abc4trust.xml.*;
import io.exonym.helpers.BuildPresentationPolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.NetworkMapItemLead;
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
                                                                ArrayList<CredentialSpecification> cSpecs,
                                                                String challengeB64) throws Exception {
        BuildPresentationPolicy bppPlain = generateBaseAlternative(verifier, cSpecs, sybilIssuer, ex, challengeB64);
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

    private static void filterSourceList(ArrayList<NetworkMapItemLead> acceptableSources, RulebookVerifier verifier) throws UxException {
        String rid = UIDHelper.computeRulebookHashUid(verifier.getRulebook().getRulebookId());
        ArrayList<NetworkMapItemLead> result = new ArrayList<>();
        for (NetworkMapItemLead source : acceptableSources){
            if (source.getLeadUID().toString().contains(rid)){
                result.add(source);
            }
        }
        acceptableSources.clear();
        acceptableSources.addAll(result);

    }

    private static BuildPresentationPolicy generateBaseAlternative(RulebookVerifier verifier,
                                                                   ArrayList<CredentialSpecification> specs,
                                                                   URI sybilIssuer,
                                                                   ExternalResourceContainer ex,
                                                                   String challengeB64) throws Exception {
        BuildPresentationPolicy bpp = new BuildPresentationPolicy(
                URI.create("urn:io:exonym:join"), ex);
        bpp.addPseudonym(SYBIL_ALIAS.toString(), false, BASE_ALIAS.toString());

        ArrayList<URI> rules = verifier.toRulebookUIDs();
        for (URI rule : rules){
            bpp.addPseudonym(rule.toString(), true, null, BASE_ALIAS);

        }
        bpp.makeInteractive(challengeB64);

        UIDHelper helper = new UIDHelper(sybilIssuer);

        List<CredentialInPolicy.IssuerAlternatives.IssuerParametersUID> issuerParametersUIDS = new ArrayList<>();
        issuerParametersUIDS.add(helper.computeIssuerParametersUID());
        bpp.addCredentialInPolicy(specs, issuerParametersUIDS, SYBIL_ALIAS.toString(), BASE_ALIAS);
        return bpp;

    }

    public static PresentationPolicy baseJoinPolicy(RulebookVerifier verifier,
                                                                URI sybilIssuer,
                                                                ExternalResourceContainer ex,
                                                                ArrayList<CredentialSpecification> cSpecs,
                                                                byte[] nonceFromMessage) throws Exception {
        return baseJoinPolicy(verifier, sybilIssuer, ex, cSpecs, Base64.encodeBytes(nonceFromMessage));
    }


}
