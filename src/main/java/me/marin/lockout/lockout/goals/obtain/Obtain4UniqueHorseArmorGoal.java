package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain4UniqueHorseArmorGoal extends ObtainSomeOfTheItemsGoal {

    private static final ItemStack ITEM_STACK = Items.DIAMOND_HORSE_ARMOR.getDefaultStack();
    static {
        ITEM_STACK.setCount(4);
    }

    private static final List<Item> ITEMS = List.of(
        Items.LEATHER_HORSE_ARMOR,
        Items.IRON_HORSE_ARMOR,
        Items.GOLDEN_HORSE_ARMOR,
        Items.DIAMOND_HORSE_ARMOR,
        Items.COPPER_HORSE_ARMOR,
        Items.NETHERITE_HORSE_ARMOR
    );

    public Obtain4UniqueHorseArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 4;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 4 unique Horse Armors";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, ITEM_STACK, x, y);
        return true;
    }

}
