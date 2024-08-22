package io.exonym.rulebook.context;
//
//import com.google.gson.JsonObject;
//import eu.abc4trust.xml.*;
//import io.exonym.abc.util.JaxbHelper;
//import io.exonym.actor.actions.ExonymOwner;
//import io.exonym.actor.actions.PkiExternalResourceContainer;
//import io.exonym.helpers.BuildPresentationPolicy;
//import io.exonym.helpers.UIDHelper;
//import io.exonym.lite.connect.Http;
//import io.exonym.lite.exceptions.ErrorMessages;
//import io.exonym.lite.exceptions.UxException;
//import io.exonym.lite.pojo.Namespace;
//import io.exonym.lite.pojo.NonInteractiveProofRequest;
//import io.exonym.lite.standard.PassStore;
//import io.exonym.lite.standard.WhiteList;
//import io.exonym.utils.storage.XContainer;
//import org.apache.commons.codec.binary.Base64;
//import org.apache.http.client.ClientProtocolException;
//
//import java.net.URI;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Path;
//import java.time.Instant;
//import java.time.Period;
//import java.util.*;
//import java.util.logging.Logger;
//
//public class Prove {
//
//    private final static Logger logger = Logger.getLogger(Prove.class.getName());
//
//    public static final ArrayList<String> BLACKLIST = new ArrayList<>();
//
//    public static final String[] BLACKLIST_PSEUDONYMS = {
//            "urn:io:exonym",
//            "urn:io:exonym:sybil",
//            "",
//            "urn",
//            "urn:",
//
//    };
//
//    public static final URI DEFAULT_ALIAS = URI.create(BLACKLIST_PSEUDONYMS[0]);
//
//    static {
//        for (String b : BLACKLIST_PSEUDONYMS) {
//            BLACKLIST.add(b);
//        }
//    }
//
//
//    protected String nonInteractiveProofRequest(String nonInteractiveProofRequestJson) throws Exception {
//        NonInteractiveProofRequest request = JaxbHelper.jsonToClass(
//                nonInteractiveProofRequestJson,
//                NonInteractiveProofRequest.class);
//        return nonInteractiveProofRequest(request);
//
//
//    }
//
//    protected Prove() throws Exception {
//
//
//    }
//
//
//    protected String nonInteractiveProofRequest(NonInteractiveProofRequest request) throws Exception {
//        List<String> issuers = new ArrayList<>(request.getIssuerUids());
//        // we are not allowing pseudonyms in non-interactive proofs before p2p use cases are targeted.
//        // this is because the use of cross-domain nyms needs to be limited.
//        List<String> nyms = new ArrayList<>();
//        if (request.getMetadata()==null){
//            throw new UxException(ErrorMessages.INSPECTION_RESULT_REQUIRED);
//
//        }
//        for (String issuer : issuers){
//            if (!issuer.endsWith(":i")){
//                throw new UxException("ISSUER_UID_REQUIRED", issuer);
//
//            }
//        }
//
//        return proofForRulebooks((ArrayList<String>) issuers,
//                (ArrayList<String>) nyms,
//                request.getMetadata());
//
//    }
//
//    /**
//     * Used for user generation of non-interactive proofs
//     * <p>
//     * <p>
//     * An ideal would be to SFTP it to the user's choice of location.
//     *
//     * @param issuerUids
//     * @param pseudonyms
//     * @param metadata
//     * @return the serialized proof token
//     * @throws Exception
//     */
//    private String proofForRulebooks(ArrayList<String> issuerUids,
//                                     ArrayList<String> pseudonyms,
//                                     JsonObject metadata) throws Exception {
//        PresentationPolicy pp = proofRequest(issuerUids, pseudonyms, metadata);
//        PresentationToken token = proveFromPresentationPolicy(pp);
//        return XContainer.convertObjectToXml(token);
//
//    }
//
//    private String proofForRulebookSSO(ArrayList<String> issuerUids,
//                                       String pseudonym,
//                                       JsonObject metadata) throws Exception {
//        try {
//            if (WhiteList.url(pseudonym)){
//                List<String> nyms = List.of(pseudonym);
//                PresentationPolicy pp = proofRequest(issuerUids, nyms, metadata, true);
//                PresentationToken token = proveFromPresentationPolicy(pp);
//                String xml = XContainer.convertObjectToXml(token);
//                String[] parts = pseudonym.split("//");
//                String protocol = parts[0];
//                String domain = parts[1].split("/")[0];
//                String target = protocol + "//" + domain + "/exonym";
//                logger.info(target);
//                Http client = new Http();
//                return client.basicPost(target, xml);
//
//            } else {
//                throw new UxException(ErrorMessages.UNEXPECTED_PSEUDONYM_REQUEST,
//                        "SSO authentications require a domain name");
//
//            }
//        } catch (ClientProtocolException e) {
//            throw new UxException(ErrorMessages.SSO_END_POINT_404, e);
//
//        }
//    }
//
//
//    protected PresentationPolicy proofRequest(ArrayList<String> issuerUids,
//                                              ArrayList<String> pseudonyms) throws Exception {
//        return proofRequest(issuerUids, pseudonyms, null, false);
//
//    }
//
//    protected PresentationPolicy proofRequest(ArrayList<String> issuerUids,
//                                              ArrayList<String> pseudonyms, JsonObject metadata) throws Exception {
//        return proofRequest(issuerUids, pseudonyms, metadata, false);
//
//    }
//
//    private PresentationPolicy proofRequest(ArrayList<String> issuerUids,
//                                            List<String> pseudonyms,
//                                            JsonObject metadata, boolean allowURLs) throws Exception {
//        URI ppUID = URI.create(Namespace.URN_PREFIX_COLON +
//                UUID.randomUUID().toString().replaceAll("-", "") + ":pp");
//
//        HashMap<URI, UIDHelper> helpers = new HashMap<>();
//        PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
//        BuildPresentationPolicy bpp = new BuildPresentationPolicy(ppUID, external);
//        if (metadata==null){
//            bpp.makeInteractive();
//
//        } else {
//            bpp.makeNonInteractive(metadata.toString());
//
//        }
//        if (!issuerUids.isEmpty()){
//            HashSet<URI> credentials = new HashSet<>();
//
//            HashMap<URI, ArrayList<CredentialInPolicy.IssuerAlternatives.IssuerParametersUID>>
//                    credentialToIssuers = new HashMap<>();
//
//            for (String issuer : issuerUids){
//                UIDHelper helper = new UIDHelper(issuer);
//                helpers.put(helper.getCredentialSpec(), helper);
//                credentials.add(helper.getCredentialSpec());
//
//                ArrayList<CredentialInPolicy.IssuerAlternatives.IssuerParametersUID>
//                        issuers = credentialToIssuers.getOrDefault(
//                        helper.getCredentialSpec(),
//                        new ArrayList<>());
//
//                issuers.add(helper.computeIssuerParametersUID());
//                credentialToIssuers.put(helper.getCredentialSpec(), issuers);
//
//            }
//            URI sybilTestnetUID = exo.getNetworkMap().nmiForSybilTestNet().getLastIssuerUID();
//            UIDHelper sybilHelper = new UIDHelper(sybilTestnetUID);
//            bpp.addCredentialInPolicy(sybilHelper.getCredentialSpecAsList(),
//                    sybilHelper.computeIssuerParametersUIDAsList(), UUID.randomUUID().toString(), DEFAULT_ALIAS);
//            CredentialSpecification sybilCS = external.openResource(sybilHelper.getCredentialSpecFileName());
//            AttributeDescription attDesc = sybilCS.getAttributeDescriptions()
//                    .getAttributeDescription().get(0);
//
//            for (URI credential : credentials){
//                UIDHelper helper = helpers.get(credential);
//
//                bpp.addCredentialInPolicy(helper.getCredentialSpecAsList(),
//                        credentialToIssuers.get(credential), UUID.randomUUID().toString(), DEFAULT_ALIAS);
//
//                bpp.addDisclosableAttributeForCredential(
//                        helper.getCredentialSpec(), attDesc,
//                        BuildPresentationPolicy.createInspectorAlternatives(helper.getInspectorParams()),
//                        "On suspected violation of rulebook");
//
//            }
//        }
//        bpp.addPseudonym(BLACKLIST_PSEUDONYMS[0], false, BLACKLIST_PSEUDONYMS[0]);
//        for (String pseudonym : pseudonyms){
//            if (verifyPseudonym(pseudonym, allowURLs)){
//                bpp.addPseudonym(pseudonym, true, null, DEFAULT_ALIAS);
//
//            }
//        }
//        return bpp.getPolicy();
//
//    }
//
//    private boolean verifyPseudonym(PresentationPolicyAlternatives ppa, boolean allowURLs) throws UxException {
//        int i = 0;
//        boolean result = true;
//        for (PresentationPolicy pp : ppa.getPresentationPolicy()){
//            for (PseudonymInPolicy nym : pp.getPseudonym()){
//                if (nym.isExclusive()){
//                    String scope = nym.getScope();
//                    if (!verifyPseudonym(scope, allowURLs)){
//                        result = false;
//
//                    }
//                    i++;
//                }
//            }
//        }
//        if ((allowURLs && i > 1) || !result){
//            throw new UxException(ErrorMessages.UNEXPECTED_PSEUDONYM_REQUEST,
//                    "Only one pseudonym is allowed for delegations");
//
//        } else {
//            return true;
//
//        }
//    }
//
//    private boolean verifyPseudonym(String pseudonym, boolean allowURLs) {
//        if (BLACKLIST.contains(pseudonym)){
//            return false;
//
//        }
//        if (WhiteList.url(pseudonym) && allowURLs){
//            return true;
//
//        } else if (pseudonym.contains(":")){
//            String[] parts = pseudonym.split(":");
//            String expiry = parts[parts.length-1];
//
//            if (WhiteList.isNumbers(expiry)){
//                long exp = Long.parseLong(expiry);
//                return DateHelper.isTargetInFutureWithinPeriod(
//                        Instant.ofEpochMilli(exp), Period.of(0,0,366));
//
//            }
//        }
//        return false;
//    }
//
//    // todo clarify use cases - then protected
//    private String proofForPolicy(String policy) throws Exception {
//        ExonymOwner owner = exo.getOwner();
//        PresentationPolicyAlternatives ppa = WalletUtils.openPPA(policy);
//        verifyPresentationPolicyAlternatives(ppa);
//        PresentationTokenDescription ptd = owner.canProveClaimFromPolicy(ppa);
//        if (ptd!=null){
//            PresentationToken proof = owner.proveClaim(ptd, ppa);
//            return XContainer.convertObjectToXml(proof);
//
//        } else {
//            return determineUnfulfilled(ptd, ppa);
//
//        }
//    }
//
//    private void verifyPresentationPolicyAlternatives(PresentationPolicyAlternatives ppa) {
//        // todo
//    }
//
//    private String determineUnfulfilled(PresentationTokenDescription ptd, PresentationPolicyAlternatives ppa) throws UxException {
//        try {
//            exo.getOwner().proveClaim(ptd, ppa);
//            return null;
//
//        } catch (UxException e) {
//            ArrayList<String> needed = e.getInfo();
//            StringBuilder b = new StringBuilder();
//            for (String n : needed){
//                b.append(n);
//
//            }
//            return b.toString();
//
//        } catch (Exception e){
//            throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE, e);
//
//        }
//    }
//
//    private <T> T jsonToClass(String ssoCJson, Class<?> clazz) throws UxException {
//        try {
//            return (T) JaxbHelper.jsonToClass(ssoCJson, clazz);
//
//        } catch (Exception e) {
//            throw new UxException(ErrorMessages.UNKNOWN_COMMAND, e, "Message class error");
//
//        }
//    }
//
//    private String decodeRequest(String challengeB64) throws UxException {
//        try {
//            byte[] decompressed = WalletUtils.decompress(Base64.decodeBase64(challengeB64));
//            return new String(decompressed, StandardCharsets.UTF_8);
//
//        } catch (Exception e) {
//            throw new UxException(ErrorMessages.UNKNOWN_COMMAND, e, "Garbage request");
//
//        }
//    }
//
//    //
//    // Accepts both polcies and authentication requests and reports on what's required.
//    //
//    protected FulfillmentReport authenticationSummaryForULink(String authRequest) throws Exception {
//        SsoChallenge c = AuthenticationWrapper.unwrapFromUniversalLink(authRequest, SsoChallenge.class);
//        logger.info(c.getChallenge());
//        return fulfillmentReport(c);
//
//    }
//
//    protected String walletReport() throws Exception {
//        return JaxbHelper.serializeToJson(
//                peekInWallet(exo.getX()), WalletReport.class);
//
//    }
//
//    private static WalletReport peekInWallet(AbstractXContainer x) throws  Exception{
//        ArrayList<String> issuers =  x.getOwnerSecretList();
//        WalletReport report = new WalletReport();
////        HashMap<String, HashSet<URI>> map = report.getRulebooksToIssuers();
//
//        for (String assetInWallet : issuers){
//
//            if (FileType.isCredential(assetInWallet)){
//                UIDHelper h =  new UIDHelper(XContainer.fileNameToUid(assetInWallet));
//                report.add(h.getRulebookUID().toString(), h.getIssuerParameters());
//
//            } else if (FileType.isSftp(assetInWallet)){
//                report.getSftpAccess().add(XContainer.fileNameToUid(assetInWallet));
//
//            }
//        }
//        return report;
//    }
//
//}
