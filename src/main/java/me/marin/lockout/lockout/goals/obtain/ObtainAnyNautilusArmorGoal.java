package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.Utility;
import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class ObtainAnyNautilusArmorGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(
            Items.IRON_NAUTILUS_ARMOR,
            Items.GOLDEN_NAUTILUS_ARMOR,
            Items.DIAMOND_NAUTILUS_ARMOR,
            Items.NETHERITE_NAUTILUS_ARMOR,
            Items.COPPER_NAUTILUS_ARMOR
    );

    public ObtainAnyNautilusArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain any Nautilus Armor";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        Utility.drawStackCount(context, x, y, String.valueOf(getAmount()));
        return true;
    }

}
