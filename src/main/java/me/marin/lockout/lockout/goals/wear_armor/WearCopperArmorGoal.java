package me.marin.lockout.lockout.goals.wear_armor;

import me.marin.lockout.lockout.interfaces.WearArmorGoal;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class WearCopperArmorGoal extends WearArmorGoal {

    private static final List<Item> ITEMS = List.of(Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS);

    public WearCopperArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Wear Full Copper Armor";
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

}
