package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.MostStatGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class HaveMostXPLevelsGoal extends Goal implements TextureProvider, MostStatGoal {

    public HaveMostXPLevelsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most XP Levels";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/more_level.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public int getStat(LockoutTeam team) {
        int max = 0;
        for (UUID uuid : ((LockoutTeamServer)team).getPlayerIds()) {
            max = Math.max(max, LockoutServer.lockout.levels.getOrDefault(uuid, 0));
        }
        return max;
    }

}
