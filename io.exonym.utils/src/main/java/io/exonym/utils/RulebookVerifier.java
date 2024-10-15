package io.exonym.utils;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Interpretation;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.RulebookDescription;
import io.exonym.lite.pojo.RulebookItem;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Regex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RulebookVerifier {


    private static final Logger logger = LogManager.getLogger(RulebookVerifier.class);
    private Rulebook rulebook = null;

    private Rulebook displayRulebook = null;
    private String rulebookJson;

    public RulebookVerifier(String rulebookPath) throws Exception {
        Path path = Path.of(rulebookPath);
        if (path.startsWith("https://")){
            throw new HubException("Use the URL constructor");

        }
        Rulebook rulebook = JaxbHelper.jsonFileToClass(path, Rulebook.class);
        verifyRulebook(rulebook);
        setupDisplay();

    }


    public RulebookVerifier(Rulebook rulebook) throws Exception {
        this.rulebook =rulebook;
        verifyRulebook(rulebook);
        setupDisplay();

    }


    public RulebookVerifier(URL rulebookUrl) throws Exception {
        this.rulebookJson = new String(UrlHelper.read(rulebookUrl), StandardCharsets.UTF_8);
        this.rulebook = JaxbHelper.jsonToClass(rulebookJson, Rulebook.class);
        verifyRulebook(rulebook);
        setupDisplay();

    }

    private void setupDisplay() throws Exception {
        this.displayRulebook = this.getDisplayRulebook();
        this.rulebookJson = JaxbHelper.serializeToJson(this.displayRulebook, Rulebook.class);

    }

    public Rulebook getDeepCopy() throws Exception {
        return JaxbHelper.jsonToClass(this.rulebookJson, Rulebook.class);
    }

    public ArrayList<URI> toRulebookUIDs(){

        if (this.rulebook!=null){
            ArrayList<URI> result = new ArrayList<>();
            ArrayList<RulebookItem> r = this.rulebook.getRules();
            for (RulebookItem item : r){
                result.add(URI.create(item.getId()));

            }
            ArrayList<RulebookItem> e = this.rulebook.getRuleExtensions();
            for (RulebookItem item : e){
                result.add(URI.create(item.getId()));

            }
            return result;
        }
        throw new RuntimeException("No rulebook");
    }

    private void verifyRulebook(Rulebook rulebook) throws UxException {
        StringBuilder builder = new StringBuilder();
        RulebookDescription d =  rulebook.getDescription();
        builder.append(d.isProduction());
        builder.append(d.getName());
        builder.append(d.getSimpleDescriptionEN());

        for (RulebookItem item : rulebook.getRules()){
            String id = item.getId();
            String parts[] = id.split(":");
            String hash = parts[4];
            String compare = CryptoUtils.computeSha256HashAsHex(item.getDescription());
            if (!hash.equals(compare)){
                throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE,
                        compare, hash, "The rule has been changed and is invalid.");

            }
            builder.append(id);

        }
        String rulebookId = CryptoUtils.computeSha256HashAsHex(builder.toString());
        String id = rulebook.getRulebookId().split(":")[3];
        if (!id.equals(rulebookId)){
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "The Rulebook ID is invalid.");

        }
    }

    public Rulebook getRulebook() {
        return rulebook;
    }

    public Rulebook getDisplayRulebook(){
        if (displayRulebook ==null){
            computeDisplayRulebook(this.rulebook);

        }
        return displayRulebook;
    }

    private void computeDisplayRulebook(Rulebook rulebook) {
        if (rulebook!=null){
            Rulebook result = new Rulebook();
            result.setRulebookId(rulebook.getRulebookId());
            result.setDescription(rulebook.getDescription());
            result.setPenalties(rulebook.getPenalties());
            result.setAcceptsSybilClasses(rulebook.getAcceptsSybilClasses());
            result.setRules(includeInterpretations(rulebook.getRules()));
            this.displayRulebook = result;

        } else {
            throw new NullPointerException();

        }
    }

    private ArrayList<RulebookItem> includeInterpretations(ArrayList<RulebookItem> rules) {
        ArrayList<RulebookItem> result = new ArrayList<>();
        for (RulebookItem rule : rules){
            result.add(applyOverrides(rule));

        }
        return result;
    }
    


    private RulebookItem applyOverrides(RulebookItem rule) {
        String origin = rule.getDescription();

        String[] fullPhrase = origin.split("_");

        if (fullPhrase.length!=1){
            return executeSwap(rule, origin, fullPhrase);

        } else {
            return rule;

        }
    }

    public void extendRulebook(RulebookItem item){
        // verify rule.
        this.rulebook.getRuleExtensions().add(item);

    }

    // TODO - Tech Debt.
    // TODO - we need a decent rulebook that tests all the possible cases, but from creation to display.
    private RulebookItem executeSwap(RulebookItem rule, String origin, String[] fullPhrase) {
        try {
            ArrayList<Interpretation> overrides = rule.getInterpretations();
            ArrayList<Interpretation> originals = new ArrayList<>();
            Pattern regex = Regex.forInterpretationInRule();
            Matcher matcher = regex.matcher(origin);
            StringBuilder builder = new StringBuilder();
            int startsWithInterp =  origin.startsWith("_") ? 1 : 0;
            int i = 0;
            for (int k=0; k<fullPhrase.length ;k++) {
                if (k%2==startsWithInterp){
                    int expectedIndex = k+startsWithInterp;
                    if (expectedIndex >= fullPhrase.length){
                        break;
                    }
                    builder.append(fullPhrase[expectedIndex]);

                } else {
                    matcher.find();
                    String original = matcher.group(0);
                    Interpretation override = overrides.get(i);
                    Interpretation displayOriginal = new Interpretation();
                    displayOriginal.setDefinition(original);
                    displayOriginal.setModifier(override.getModifier());
                    originals.add(displayOriginal);
                    String def = override.getDefinition();
                    String highlightOpen = (!original.contains(def) ? "<b>" : "<i>");
                    String highlightClose = (highlightOpen.equals("<b>") ? "</b>" : "</i>");
                    builder.append(highlightOpen);
                    builder.append(override.getDefinition());
                    builder.append(highlightClose);
                    i++;

                }
            }
            RulebookItem result = new RulebookItem();
            result.setDescription(builder.toString());
            result.setId(rule.getId());
            result.setInterpretations(originals);
            return result;

        } catch (Exception e) {
            logger.error("Display Rulebook Error", e);
            return rule;

        }
    }

    public static Rulebook fromString(String json) throws Exception {
        return JaxbHelper.jsonToClass(json, Rulebook.class);
    }


    public boolean isSybil(){
        return Rulebook.isSybil(rulebook.getRulebookId());
    }

    public static void main(String[] args) throws Exception {
        String url = "https://trust.exonym.io/sybil-rulebook.json";
        RulebookVerifier v = new RulebookVerifier(new URL(url));
        System.out.println(v.getDisplayRulebook());


    }

}
