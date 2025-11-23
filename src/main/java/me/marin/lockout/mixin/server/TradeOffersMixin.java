package me.marin.lockout.mixin.server;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TradeOffers.class)
public class TradeOffersMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void lockout$forceTrialChamberMap(CallbackInfo ci) {
        Int2ObjectMap<TradeOffers.Factory[]> cartographerTrades = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(VillagerProfession.CARTOGRAPHER);
        if (cartographerTrades != null) {
            cartographerTrades.put(2, new TradeOffers.Factory[]{
                new TradeOffers.BuyItemFactory(Items.GLASS_PANE, 11, 12, 10),
                new TradeOffers.SellMapFactory(12, StructureTags.ON_TRIAL_CHAMBERS_MAPS, "filled_map.trial_chambers", MapDecorationTypes.TRIAL_CHAMBERS, 12, 10)
            });
            cartographerTrades.put(3, new TradeOffers.Factory[]{
                new TradeOffers.BuyItemFactory(Items.COMPASS, 1, 12, 20),
                new TradeOffers.SellMapFactory(13, StructureTags.ON_OCEAN_EXPLORER_MAPS, "filled_map.monument", MapDecorationTypes.MONUMENT, 12, 10)
            });
        }
    }
}
