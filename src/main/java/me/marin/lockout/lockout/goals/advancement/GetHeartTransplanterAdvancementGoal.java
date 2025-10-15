package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetHeartTransplanterAdvancementGoal extends AdvancementGoal {

    private static final ItemStack ITEM_STACK = Blocks.CREAKING_HEART.asItem().getDefaultStack();
    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "adventure/heart_transplanter"));

    public GetHeartTransplanterAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Achieve \"Heart Transplanter\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
