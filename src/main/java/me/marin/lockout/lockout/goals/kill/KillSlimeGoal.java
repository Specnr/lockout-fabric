package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class KillSlimeGoal extends KillMobGoal implements TextureProvider {

    public KillSlimeGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill Slime";
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/slime.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public EntityType<?> getEntity() {
        return EntityType.SLIME;
    }
    
    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier OVERLAY = Identifier.of(Constants.NAMESPACE, "textures/custom/overlay/kill_overlay.png");
    
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }
}

