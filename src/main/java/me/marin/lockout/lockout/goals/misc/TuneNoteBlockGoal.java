package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.List;

public class TuneNoteBlockGoal extends IncrementStatGoal {

    public TuneNoteBlockGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> STATS = List.of(Stats.TUNE_NOTEBLOCK);
    @Override
    public List<Identifier> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Tune a Note Block";
    }

    private static final ItemStack ITEM_STACK = Items.NOTE_BLOCK.getDefaultStack();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
