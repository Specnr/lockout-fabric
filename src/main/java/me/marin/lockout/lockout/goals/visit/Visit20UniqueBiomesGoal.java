package me.marin.lockout.lockout.goals.visit;

import me.marin.lockout.lockout.interfaces.VisitUniqueBiomesGoal;

public class Visit20UniqueBiomesGoal extends VisitUniqueBiomesGoal {

    public Visit20UniqueBiomesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Visit 20 Unique Biomes";
    }

    @Override
    public int getAmount() {
        return 20;
    }
}
