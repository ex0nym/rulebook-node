package io.exonym.actor.actions;

import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.smartcard.Base64;
import eu.abc4trust.xml.*;
import io.exonym.helpers.CredentialWrapper;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.AttributeBasedTokenResult;
import io.exonym.lite.pojo.DecodedAttributeToken;
import io.exonym.utils.storage.CacheContainer;
import io.exonym.managers.KeyManagerSingleton;
import io.exonym.utils.storage.ExternalResourceContainer;

import java.net.URI;
import java.util.List;

public class TokenVerifier {

	private final ExonymOwner owner;
	private final KeyManagerSingleton keyManager = KeyManagerSingleton.getInstance();

	private final ExternalResourceContainer external;


	public TokenVerifier(CacheContainer cache, ExternalResourceContainer external) throws Exception {
		this.owner = new ExonymOwner(cache.getContainer());
		this.owner.initSystemParameters();
		this.external=external;

	}

	public void load(UIDHelper helper, NodeVerifier source, NodeVerifier advocate) throws Exception {
		this.loadCredentialSpecification(source.getCredentialSpecification());
		this.loadIssuerParameters(advocate.getIssuerParameters(helper.getIssuerParametersFileName()));

		this.loadRevocationAuthorityParameters(
				advocate.getRevocationAuthorityParameters(
						helper.getRevocationInformationFileName()));

		this.loadRevocationInformation(
				advocate.getRevocationInformation(
						helper.getRevocationInformationFileName()));

		this.loadInspectorParams(advocate.getInspectorPublicKey());


	}

	public void loadCredentialSpecification(URI credentialSpecUid) throws Exception {
		if (keyManager.getCredentialSpecification(credentialSpecUid)==null){
			keyManager.storeCredentialSpecification(credentialSpecUid, external.openResource(credentialSpecUid));
		}
	}

	public void loadCredentialSpecification(CredentialSpecification credentialSpec){
		if (credentialSpec==null){
			throw new NullPointerException();

		}
		this.owner.addCredentialSpecification(credentialSpec);

	}

	public void loadIssuerParameters(URI issuerParametersUid) throws Exception {
		if (keyManager.getIssuerParameters(issuerParametersUid)==null){
			keyManager.storeIssuerParameters(issuerParametersUid, external.openResource(issuerParametersUid));
		}
	}

	public void loadIssuerParameters(IssuerParameters issuerParameters) throws Exception {
		if (issuerParameters==null){
			throw new NullPointerException();

		}
		this.owner.addIssuerParameters(issuerParameters);

	}

	public void loadRevocationAuthorityParameters(RevocationAuthorityParameters rap) throws KeyManagerException {
		if (rap==null){
			throw new NullPointerException();

		}
		this.owner.addRevocationAuthorityParameters(rap);

	}

	public void loadRevocationAuthorityParameters(URI rapUid) throws Exception {
		if (keyManager.getRevocationAuthorityParameters(rapUid)==null){
			keyManager.storeRevocationAuthorityParameters(rapUid, external.openResource(rapUid));
		}
	}

	public void loadRevocationInformation(URI rap, URI ri) throws Exception {
		if (keyManager.getRevocationInformation(rap, ri)==null){
			this.owner.addRevocationInformation(rap, external.openResource(ri));

		}
	}


	public void loadRevocationInformation(RevocationInformation revocationInformation) throws Exception {
		if (revocationInformation==null){
			throw new NullPointerException();

		}
		URI rai = revocationInformation.getRevocationInformationUID();
		this.owner.addRevocationInformation(rai, revocationInformation);

	}

	public void loadInspectorParams(URI ins) throws Exception {
		if (keyManager.getInspectorPublicKey(ins)==null){
			this.owner.addInspectorParameters(external.openResource(ins));

		}
	}

	public void loadInspectorParams(InspectorPublicKey ins) throws Exception {
		if (ins==null){
			throw new NullPointerException();

		}
		this.owner.addInspectorParameters(ins);

	}

	public byte[] verifyToken(PresentationPolicyAlternatives ppa, PresentationToken pt) throws Exception {
		if (ppa==null){
			throw new NullPointerException();

		}
		if (pt==null){
			throw new NullPointerException();

		}
		if (this.owner.verifyClaim(ppa, pt)) {
			return pt.getPresentationTokenDescription().getMessage().getNonce();

		} else {
			throw new UxException("The token was invalid");

		}
	}

	public AttributeBasedTokenResult verifyTokenWithAttributes(PresentationPolicyAlternatives ppa, PresentationToken pt) throws Exception {
		if (ppa==null){
			throw new NullPointerException();

		}
		if (pt==null){
			throw new NullPointerException();

		}
		if (this.owner.verifyClaim(ppa, pt)) {
			AttributeBasedTokenResult result = new AttributeBasedTokenResult();
			byte[] messgage = pt.getPresentationTokenDescription().getMessage().getNonce();
			byte[] expected = ppa.getPresentationPolicy().get(0).getMessage().getNonce();
			String m = Base64.encodeBytes(messgage);
			String e = Base64.encodeBytes(expected);
			if (m.equals(e)){
				result.setMessage(messgage);
				List<CredentialInToken> credentials = pt.getPresentationTokenDescription().getCredential();

				for (CredentialInToken c : credentials) {
					CredentialSpecification spec = external.openResource(c.getCredentialSpecUID());;
					DecodedAttributeToken dat = CredentialWrapper.decodeAttributes(c.getDisclosedAttribute(), spec);
					result.getDisclosedAttributes().add(dat);

				}
				return result;

			} else {
				throw new UxException(ErrorMessages.TOKEN_INVALID, "Message does not match expected");

			}
		} else {
			throw new UxException("The token was invalid");


		}
	}

	// static Verify with load

}
