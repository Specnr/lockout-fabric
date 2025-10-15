package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetStayHydratedAdvancementGoal extends AdvancementGoal {

    private static final ItemStack ITEM_STACK = Items.DRIED_GHAST.getDefaultStack();
    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "husbandry/place_dried_ghast_in_water"));

    public GetStayHydratedAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Hydrate a dried ghast";
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
