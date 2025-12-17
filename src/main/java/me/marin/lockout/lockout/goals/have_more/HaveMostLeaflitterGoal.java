package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.MostStatGoal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class HaveMostLeaflitterGoal extends Goal implements CustomTextureRenderer, MostStatGoal {

    private static final ItemStack ITEM_STACK = Items.LEAF_LITTER.getDefaultStack();
    
    public HaveMostLeaflitterGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most Leaflitter";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/overlay/up_overlay.png");
    
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawItem(ITEM_STACK, x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }

    @Override
    public int getStat(LockoutTeam team) {
        int max = 0;
        for (UUID uuid : ((LockoutTeamServer)team).getPlayers()) {
            max = Math.max(max, LockoutServer.lockout.playerLeaflitterCounts.getOrDefault(uuid, 0));
        }
        return max;
    }
}

