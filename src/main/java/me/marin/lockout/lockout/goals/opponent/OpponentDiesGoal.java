package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.OpponentGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class OpponentDiesGoal extends Goal implements TextureProvider, OpponentGoal {

    public OpponentDiesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "All other opponents die";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/opponent/no_death.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}