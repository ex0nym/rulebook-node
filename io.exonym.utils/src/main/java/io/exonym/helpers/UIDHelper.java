package io.exonym.helpers;

import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInToken;
import eu.abc4trust.xml.PresentationToken;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.storage.XContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class UIDHelper {


    private static final Logger logger = LogManager.getLogger(UIDHelper.class);
    private String sourceName;
    private String advocateName;
    private  URI credentialSpec;
    private  URI presentationPolicy;
    private  URI issuedCredential;
    private  URI revocationAuthority;
    private  URI revocationAuthorityInfo;
    private  URI issuancePolicy;
    private  URI issuerParameters;
    private  URI inspectorParams;

    private  URI rulebookUID;

    private  String credentialSpecFileName;
    private  String presentationPolicyFileName;
    private  String issuedCredentialFileName;
    private  String revocationAuthorityFileName;
    private  String revocationInfoFileName;
    private  String issuancePolicyFileName;
    private  String issuerParametersFileName;
    private  String inspectorParamsFileName;
    private  String rulebookFileName;

    private  URI nodeUid;

    private  URI sourceUid;


    public UIDHelper(URI issuerParameters) throws Exception {
        assemble(issuerParameters.toString());

    }

    public UIDHelper(String issuerParameters) throws Exception {
        assemble(issuerParameters);

    }

    private void assemble(String issuerParameters) throws Exception {
        String[] parts = issuerParameters.split(":");
        String b0 = parts[0] + ":" +parts[1] + ":" +parts[2] + ":" +parts[3] + ":" +parts[4];
        String base = parts[0] + ":" +parts[1] + ":" +parts[2] + ":" +parts[4];

        this.sourceName = parts[2];
        this.advocateName = parts[3];
        this.sourceUid = URI.create(base);
        this.nodeUid = URI.create(b0);
        this.issuerParameters = URI.create(issuerParameters);
        this.issuerParametersFileName = XContainer.uidToXmlFileName(issuerParameters);
        this.rulebookUID = URI.create(Namespace.URN_PREFIX_COLON + parts[4]);

        String root = Namespace.URN_PREFIX_COLON  + XContainer.stripUidSuffix(this.issuerParameters, 1);

        issuedCredential = URI.create(root + ":ic");
        issuedCredentialFileName  = XContainer.uidToXmlFileName(issuedCredential);

        this.presentationPolicy = URI.create(base + ":pp");
        presentationPolicyFileName = XContainer.uidToXmlFileName(presentationPolicy);

        revocationAuthority = URI.create(root + ":ra");
        revocationAuthorityFileName = XContainer.uidToXmlFileName(revocationAuthority);

        revocationAuthorityInfo = URI.create(root + ":rai");
        revocationInfoFileName = XContainer.uidToXmlFileName(revocationAuthorityInfo);

        issuancePolicy = URI.create(root + ":ip");
        issuancePolicyFileName = XContainer.uidToXmlFileName(issuancePolicy);

        this.inspectorParams = URI.create(b0 + ":ins");
        inspectorParamsFileName = XContainer.uidToXmlFileName(inspectorParams);

        this.credentialSpec = credentialSpecFromSourceUID(this.sourceUid);
        credentialSpecFileName = XContainer.uidToXmlFileName(credentialSpec);

        this.rulebookFileName = XContainer.uidToFileName(rulebookUID) + ".json";

    }

    public static String computeSourceNameFromAdvocateOrSourceUid(URI sourceOrAdvocateUid){
        if (sourceOrAdvocateUid==null){
            throw new NullPointerException();
        }
        return sourceOrAdvocateUid.toString().split(":")[2];
    }

    public static String computeAdvocateNameFromAdvocateUid(URI advocateUid){
        if (advocateUid==null){
            throw new NullPointerException();
        }
        return advocateUid.toString().split(":")[3];
    }

    public static String computeRulebookHashFromSourceUid(URI sourceUid){
        if (sourceUid==null){
            return null;
        }
        return sourceUid.toString().split(":")[3];
    }

    public static String computeRulebookHashFromRulebookId(String rulebookId){
        return rulebookId.split(":")[2];
    }

    public static String computeRulebookIdFromAdvocateUid(URI advocateUid){
        if (advocateUid==null){
            throw new NullPointerException();
        }
        return advocateUid.toString().split(":")[4];
    }


    public static URI credentialSpecFromSourceUID(URI sourceUid){
        String[] parts = sourceUid.toString().split(":");
        return URI.create(Namespace.URN_PREFIX_COLON + parts[3] + ":c");

    }

    public static String stripPrefix(URI uid){
        return stripPrefix(uid.toString());

    }

    public static String stripPrefix(String uid){
        return uid.replaceAll(Namespace.URN_PREFIX_COLON, "");

    }

    public static boolean isSourceUid(URI uid){
        return WhiteList.isSourceUid(uid);

    }

    public static boolean isAdvocateUid(URI uid){
        return WhiteList.isAdvocateUid(uid);

    }

    public static String uidToFileName(String uri) throws Exception{
        return XContainer.uidToFileName(uri);
    }

    public CredentialInPolicy.IssuerAlternatives.IssuerParametersUID computeIssuerParametersUID(){
        CredentialInPolicy.IssuerAlternatives.IssuerParametersUID params = new
                CredentialInPolicy.IssuerAlternatives.IssuerParametersUID();
        params.setValue(this.getIssuerParameters());
        params.setRevocationInformationUID(this.getRevocationInfoParams());
        return params;
    }

    public static CredentialInPolicy.IssuerAlternatives.IssuerParametersUID computeIssuerParametersUID(URI issuerUID){
        URI r = URI.create(issuerUID.toString().replaceAll(":i", ":rai"));
        CredentialInPolicy.IssuerAlternatives.IssuerParametersUID params = new
                CredentialInPolicy.IssuerAlternatives.IssuerParametersUID();
        params.setValue(issuerUID);
        params.setRevocationInformationUID(r);
        return params;
    }


    public static URI fileNameToUid(String filename) throws Exception{
        return URI.create(XContainer.fileNameToUid(filename));
    }


    public URI getRulebookUID() {
        return rulebookUID;
    }

    public String getRevocationInfoFileName() {
        return revocationInfoFileName;
    }

    public String getRulebookFileName() {
        return rulebookFileName;
    }

    public URI getCredentialSpec() {
        return credentialSpec;
    }

    public URI getIssuedCredential() {
        return issuedCredential;
    }

    public URI getRevocationAuthority() {
        return revocationAuthority;
    }

    public URI getRevocationInfoParams() {
        return revocationAuthorityInfo;
    }

    public URI getIssuancePolicy() {
        return issuancePolicy;
    }

    public URI getIssuerParameters() {
        return issuerParameters;
    }

    public URI getPresentationPolicy() {
        return presentationPolicy;
    }

    public URI getInspectorParams() {
        return inspectorParams;
    }

    public URI getNodeUid() {
        return nodeUid;
    }

    public URI getSourceUid() {
        return sourceUid;
    }

    public String getCredentialSpecFileName() {
        return credentialSpecFileName;
    }

    public String getPresentationPolicyFileName() {
        return presentationPolicyFileName;
    }

    public String getIssuedCredentialFileName() {
        return issuedCredentialFileName;
    }

    public String getRevocationAuthorityFileName() {
        return revocationAuthorityFileName;
    }

    public String getRevocationInformationFileName() {
        return revocationInfoFileName;
    }

    public String getIssuancePolicyFileName() {
        return issuancePolicyFileName;
    }

    public String getIssuerParametersFileName() {
        return issuerParametersFileName;
    }

    public String getInspectorParamsFileName() {
        return inspectorParamsFileName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getAdvocateName() {
        return advocateName;
    }

    public static URI computeAdvocateUidFromMaterialUID(URI advocateMaterialUID) throws Exception {
        if (advocateMaterialUID==null){
            throw new NullPointerException();
        }
        String[] parts = advocateMaterialUID.toString().split(":");
        StringBuilder result = new StringBuilder();
        result.append(Namespace.URN_PREFIX_COLON);
        result.append(parts[2]);
        result.append(":");
        result.append(parts[3]);
        result.append(":");
        result.append(parts[4]);
        URI advocateUid = URI.create(result.toString());
        if (WhiteList.isAdvocateUid(advocateUid)) {
            return advocateUid;

        } else {
            throw new UxException(advocateUid.toString());

        }
    }
    /**
     *
     * @param nodeUid
     * @return [sourceUid, nodeUid]
     * @throws Exception
     */
    public static URI computeSourceUidFromNodeUid(URI nodeUid) throws Exception {
        try {
            if (nodeUid!=null){
                String[] discovery = nodeUid.toString().split(":");
                if (discovery.length==5) {
                    URI sourceUid = URI.create(
                            discovery[0] + ":"
                                    + discovery[1] + ":"
                                    + discovery[2] + ":"
                                    + discovery[4]);
                    return sourceUid;

                } else if (discovery.length==4){
                    return nodeUid;

                } else {
                    throw new HubException("Invalid Node UID (5 components expected)" + nodeUid + " got " + discovery.length);

                }
            } else {
                throw new NullPointerException();

            }
        } catch (Exception e){
            throw e;

        }
    }

    public static String tokenFileName(URI advocateUid) throws Exception {
        return DateHelper.currentIsoUtcDateTime()
                + "-" + XContainer.uidToFileName(advocateUid)
                + ".t.xml";
    }

    public static URI extractFirstIssuerFromPresentationToken(PresentationToken token){
        CredentialInToken credential = token.getPresentationTokenDescription().getCredential().get(0);
        credential.getCredentialSpecUID();
        return credential.getIssuerParametersUID();

    }



    public void out(){
        logger.info(">>>>>>>>>>>>> ");
        logger.info("> Outputting Projected UIDS");
        logger.info("");
        logger.info(sourceName);
        logger.info(advocateName);
        logger.info(sourceUid);
        logger.info(nodeUid);
        logger.info(rulebookUID);
        logger.info(rulebookFileName);
        logger.info(credentialSpec);
        logger.info(credentialSpecFileName);
        logger.info(presentationPolicy);
        logger.info(presentationPolicyFileName);
        logger.info(issuedCredential);
        logger.info(issuedCredentialFileName);
        logger.info(revocationAuthority);
        logger.info(revocationAuthorityFileName);
        logger.info(revocationAuthorityInfo);
        logger.info(revocationInfoFileName);
        logger.info(issuancePolicy);
        logger.info(issuancePolicyFileName);
        logger.info(inspectorParams);
        logger.info(inspectorParamsFileName);
        logger.info(issuerParameters);
        logger.info(issuerParametersFileName);
        logger.info("");
        logger.info(">>>>>>>>>>>>> ");

    }
}
