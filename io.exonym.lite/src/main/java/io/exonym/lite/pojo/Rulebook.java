package io.exonym.lite.pojo;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

public class Rulebook extends SerialErrorHandling {


    public static final String SYBIL_URL_MAIN = "https://trust.exonym.io/sybil-rulebook.json";
    public static final String SYBIL_URL_TEST = "https://trust.exonym.io/sybil-rulebook-test.json";

    public static final String SYBIL_RULEBOOK_NAME = "sybil";
    public static final String SYBIL_LEAD = "the-cyber";
    public static final String SYBIL_MOD_TEST = "thirty-test";
    public static final String SYBIL_MOD_MAIN = "thirty";

    public static final String SYBIL_RULEBOOK_HASH_TEST =
            "2c859cff31d5889ab75027713926056323e6aeebe0fbee6bd126aae12713257c";

    public static final String SYBIL_RULEBOOK_HASH_MAIN =
            "996a36c1d3fcfcc831bca4f9061110ebdd738a2b9389929b86fb152e4211ced0";

    public static final URI SYBIL_RULEBOOK_UID_TEST = URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_RULEBOOK_HASH_TEST);

    public static final URI SYBIL_RULEBOOK_UID_MAIN = URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_RULEBOOK_HASH_MAIN);

    public static final URI SYBIL_LEAD_UID_TEST = URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_LEAD + ":"
            + SYBIL_RULEBOOK_HASH_TEST);

    public static final URI SYBIL_LEAD_UID_MAIN = URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_LEAD + ":"
            + SYBIL_RULEBOOK_HASH_MAIN);

    public static final URI SYBIL_MOD_UID_TEST =  URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_LEAD + ":"
            + SYBIL_MOD_TEST + ":"
            + SYBIL_RULEBOOK_HASH_TEST);

    public static final URI SYBIL_MOD_UID_MAIN =  URI.create(Namespace.URN_PREFIX_COLON
            + SYBIL_RULEBOOK_NAME + ":"
            + SYBIL_LEAD + ":"
            + SYBIL_MOD_MAIN + ":"
            + SYBIL_RULEBOOK_HASH_MAIN);

    public static final String SYBIL_CLASS_PERSON = "person";
    public static final String SYBIL_CLASS_ENTITY = "entity";
    public static final String SYBIL_CLASS_ROBOT = "robot";
    public static final String SYBIL_CLASS_PRODUCT = "product";
    public static final String SYBIL_CLASS_REPRESENTATIVE = "representative";

    private static final ArrayList<String> sybilClasses = new ArrayList<>();

    public static ArrayList<String> sybilClasses(){
        if (sybilClasses.isEmpty()){
            sybilClasses.add(SYBIL_CLASS_PERSON);
            sybilClasses.add(SYBIL_CLASS_ENTITY);
            sybilClasses.add(SYBIL_CLASS_REPRESENTATIVE);
            sybilClasses.add(SYBIL_CLASS_ROBOT);
            sybilClasses.add(SYBIL_CLASS_PRODUCT);

        }
        return sybilClasses;

    }

    public static final String SYBIL_CLASS_TYPE = Namespace.URN_PREFIX_COLON + "sybil-class";
    private String rulebookId;
    private RulebookDescription description;
    private ArrayList<RulebookItem> rules = new ArrayList<>();
    private ArrayList<RulebookItem> ruleExtensions = new ArrayList<>();

    private String challengeB64;
    private String link;

    private ArrayList<String> acceptsSybilClasses = new ArrayList<>();

    private RulebookPenalties penalties;

    public Rulebook(){
        this.rulebookId = UUID.randomUUID()
                .toString().replaceAll("-", "");
        this.acceptsSybilClasses.add("all");
        this.penalties = new RulebookPenalties();
    }

    public String getRulebookId() {
        return rulebookId;
    }

    public URI computeCredentialSpecId() {
        String[] parts = rulebookId.split(":");
        return URI.create(Namespace.URN_PREFIX_COLON +
                this.description.getName().toLowerCase() + ":"
                +  parts[parts.length-1] + ":c");
    }

    public void setRulebookId(String rulebookId) {
        this.rulebookId = rulebookId;
    }

    public ArrayList<RulebookItem> getRules() {
        return rules;
    }

    public void setRules(ArrayList<RulebookItem> rules) {
        this.rules = rules;
    }

    public static boolean isSybil(URI uid){
        return Rulebook.isSybil(uid.toString());

    }

    public static boolean isSybil(String rulebookId){
        if (rulebookId==null){
            return false;

        } else {
            return rulebookId.contains(SYBIL_RULEBOOK_HASH_MAIN) ||
                    rulebookId.contains(SYBIL_RULEBOOK_HASH_TEST);

        }
    }

    public static boolean isSybilMain(String rulebookId){
        if (rulebookId==null){
            return false;

        } else {
            return rulebookId.contains(SYBIL_RULEBOOK_HASH_MAIN);

        }
    }

    public static boolean isSybilTest(String rulebookId){
        if (rulebookId==null){
            return false;

        } else {
            return rulebookId.contains(SYBIL_RULEBOOK_HASH_TEST);

        }
    }


    public RulebookDescription getDescription() {
        return description;
    }

    public void setDescription(RulebookDescription description) {
        this.description = description;
    }

    public ArrayList<RulebookItem> getRuleExtensions() {
        return ruleExtensions;
    }

    public void setRuleExtensions(ArrayList<RulebookItem> ruleExtensions) {
        this.ruleExtensions = ruleExtensions;
    }

    public ArrayList<String> getAcceptsSybilClasses() {
        return acceptsSybilClasses;
    }

    public void setAcceptsSybilClasses(ArrayList<String> acceptsSybilClasses) {
        this.acceptsSybilClasses = acceptsSybilClasses;
    }

    public RulebookPenalties getPenalties() {
        return penalties;
    }

    public void setPenalties(RulebookPenalties penalties) {
        this.penalties = penalties;
    }

    public String getChallengeB64() {
        return challengeB64;
    }

    public void setChallengeB64(String challengeB64) {
        this.challengeB64 = challengeB64;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
