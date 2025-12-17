package me.marin.lockout.lockout.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public abstract class OpponentObtainsItemGoal extends ObtainAllItemsGoal implements OpponentGoal {

    public OpponentObtainsItemGoal(String id, String data) {
        super(id, data);
    }

    public abstract String getMessage(PlayerEntity player);

}
