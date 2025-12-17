package me.marin.lockout.lockout.goals.visit;

import me.marin.lockout.lockout.interfaces.VisitUniqueBiomesGoal;

public class Visit15UniqueBiomesGoal extends VisitUniqueBiomesGoal {

    public Visit15UniqueBiomesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Visit 15 Unique Biomes";
    }

    @Override
    public int getAmount() {
        return 15;
    }
}
