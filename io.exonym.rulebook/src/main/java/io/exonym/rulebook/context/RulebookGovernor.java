package io.exonym.rulebook.context;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.Penalty;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.RulebookDescription;
import io.exonym.lite.pojo.RulebookItem;
import io.exonym.utils.RulebookVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RulebookGovernor {

    private static final Logger logger = LogManager.getLogger(RulebookGovernor.class);
    private Rulebook rulebook;
    private Penalty defaultPenalty;
    private ConcurrentHashMap<String, RulebookItem> allManagedRules = new ConcurrentHashMap<>();

    public RulebookGovernor(Rulebook rulebook) throws Exception {
        this.rulebook = rulebook;
        verifyRulebook(this.rulebook);

        // TODO --->
        // find lead
        // open their rulebook; and apply all penalties

        // TODO 2 --->
        // open my rulebook; and apply all ~~allowed~~ interpretations
        // ~~Allow for a rulebook to be added to further interpret~~
        // ~~Allow for a verified rulebook to be extracted~~

    }

    private void verifyRulebook(Rulebook rulebook) throws Exception {
        new RulebookVerifier(rulebook);
        RulebookDescription desc = rulebook.getDescription();
        defaultPenalty = desc.getDefaultPenalty();

        if (defaultPenalty==null){
            defaultPenalty  = new Penalty();
            defaultPenalty.setType(Penalty.TYPE_TIME_BAN);
            defaultPenalty.setDenomination(Penalty.DEN_TEMP_DAYS);
            defaultPenalty.setQuantity(5);
            defaultPenalty.setRepeatOffenceMultiplier(2);

        }
        addRules(rulebook.getRules());
        addRules(rulebook.getRuleExtensions());

    }

    public void addRules(ArrayList<RulebookItem> rules) {
        for (RulebookItem rule : rules){
            // TODO verify rule
            allManagedRules.put(rule.getId(), rule);
        }
    }

    public RulebookItem getRule(String ruleId) {
        return allManagedRules.get(ruleId);

    }

    public void out(){
        String json = JaxbHelper.gson.toJson(this.allManagedRules);
        logger.info(json);
        logger.info("Default: " + defaultPenalty);

    }

    public Penalty getPenalty(String ruleId, int offenceCount){
        HashMap<String, Integer> rules = new HashMap<>();
        rules.put(ruleId, offenceCount);
        return getPenaltiesMaxIndex0(rules).get(0);
    }

    public ArrayList<Penalty> getPenaltiesMaxIndex0(HashMap<String, Integer> ruleIdsToOffence){
        ArrayList<Penalty> penalties = new ArrayList<>();
        for (String rule : ruleIdsToOffence.keySet()) {
            RulebookItem r = this.allManagedRules.get(rule);
            Penalty penalty = r.getPenalty();
            if (penalty!=null){
                penalty.setOffenceCount(
                        ruleIdsToOffence.get(rule));
                penalties.add(penalty);

            }
        }
        if (penalties.isEmpty()){
            penalties.add(defaultPenalty);

        } else {
            Collections.sort(penalties, Collections.reverseOrder());

        }
        return penalties;

    }

}
