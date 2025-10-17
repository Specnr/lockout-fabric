package me.marin.lockout;

import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static me.marin.lockout.lockout.GoalType.*;

public class GoalPoolConfig {

    private static final Path CONFIG_PATH = new File("./config/goal-pool.yml").toPath();

    @Getter
    private static GoalPoolConfig instance;

    private static final Yaml YAML;

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        YAML = new Yaml(options);
    }

    private final Map<String, Boolean> goalStates = new LinkedHashMap<>();

    // Goals that should be disabled by default (NOT_IN_RANDOM_POOL or TEAMS_GOAL_NOT_IN_RANDOM_POOL goals)
    private static final Set<String> DEFAULT_DISABLED_GOALS = Set.of(
            OBTAIN_WITHER_SKELETON_SKULL,
            EAT_COOKIE,
            GET_LEVITATION_STATUS_EFFECT,
            GET_A_TERRIBLE_FORTRESS_ADVANCEMENT,
            GET_THE_CITY_AT_THE_END_OF_THE_GAME_ADVANCEMENT,
            GET_THOSE_WERE_THE_DAYS_ADVANCEMENT,
            OPPONENT_OBTAINS_CRAFTING_TABLE,
            OPPONENT_OBTAINS_OBSIDIAN,
            OPPONENT_OBTAINS_SEEDS,
            OPPONENT_HIT_BY_EGG,
            OPPONENT_EATS_FOOD,
            HAVE_MOST_PLAYER_KILLS
    );

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            createConfigDir();
            loadDefaultConfig();
            save();
        } else {
            try {
                String content = Files.readString(CONFIG_PATH);
                Map<String, Object> yamlData = YAML.load(content);
                
                if (yamlData == null) {
                    loadDefaultConfig();
                } else {
                    instance = new GoalPoolConfig();
                    
                    // Parse the YAML data into goal states
                    List<String> allGoals = getAllRegisteredGoals();
                    int enabledCount = 0;
                    int disabledCount = 0;
                    
                    for (String goal : allGoals) {
                        Object value = yamlData.get(goal);
                        if (value instanceof Boolean) {
                            boolean enabled = (Boolean) value;
                            instance.goalStates.put(goal, enabled);
                            if (enabled) enabledCount++;
                            else disabledCount++;
                        } else {
                            // Default to enabled if not found or invalid
                            boolean enabled = !DEFAULT_DISABLED_GOALS.contains(goal);
                            instance.goalStates.put(goal, enabled);
                            if (enabled) enabledCount++;
                            else disabledCount++;
                        }
                    }
                    
                    Lockout.log("GoalPoolConfig loaded: " + enabledCount + " enabled, " + disabledCount + " disabled goals");
                    
                    // Ensure all registered goals are present
                    addMissingGoals();
                }
                // Don't save here - it overwrites user changes
            } catch (Exception e) {
                Lockout.log("Invalid goal-pool.yml file, using default values.");
                Lockout.error(e);
                loadDefaultConfig();
            }
        }
    }

    public static void loadDefaultConfig() {
        instance = new GoalPoolConfig();
        
        // Initialize all goals as enabled by default, except those marked as NOT_IN_RANDOM_POOL
        List<String> allGoals = getAllRegisteredGoals();
        for (String goal : allGoals) {
            instance.goalStates.put(goal, !DEFAULT_DISABLED_GOALS.contains(goal));
        }
    }

    private static void addMissingGoals() {
        List<String> allGoals = getAllRegisteredGoals();
        for (String goal : allGoals) {
            if (!instance.goalStates.containsKey(goal)) {
                instance.goalStates.put(goal, !DEFAULT_DISABLED_GOALS.contains(goal));
            }
        }
    }

    private static List<String> getAllRegisteredGoals() {
        return categorizeGoals().values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public boolean isGoalEnabled(String goalId) {
        return goalStates.getOrDefault(goalId, true);
    }

    public void setGoalEnabled(String goalId, boolean enabled) {
        goalStates.put(goalId, enabled);
    }

    public Map<String, Boolean> getGoalStates() {
        return Collections.unmodifiableMap(goalStates);
    }

    private static void createConfigDir() {
        try {
            Files.createDirectories(Path.of("./config"));
        } catch (Exception e) {
            Lockout.error(e);
        }
    }

    public static void save() {
        try {
            String yamlContent = instance.generateYamlContent();
            Files.writeString(CONFIG_PATH, yamlContent);
        } catch (Exception e) {
            Lockout.error(e);
        }
    }

    private String generateYamlContent() {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Lockout Goal Pool Configuration\n");
        yaml.append("# Edit this file to enable/disable goals in the random pool\n");
        yaml.append("# Set to 'true' to enable, 'false' to disable\n");
        yaml.append("# Goals marked as 'false' will not appear in randomly generated boards\n\n");

        // Group goals by category for better organization
        Map<String, List<String>> categorizedGoals = categorizeGoals();
        
        for (Map.Entry<String, List<String>> entry : categorizedGoals.entrySet()) {
            String category = entry.getKey();
            List<String> goals = entry.getValue();
            
            yaml.append("# ").append(category).append("\n");
            for (String goal : goals) {
                boolean enabled = goalStates.getOrDefault(goal, true);
                yaml.append(goal).append(": ").append(enabled).append("\n");
            }
            yaml.append("\n");
        }
        
        return yaml.toString();
    }

    private static Map<String, List<String>> categorizeGoals() {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        
        // Tools
        categories.put("Tools", List.of(
                OBTAIN_WOODEN_TOOLS, OBTAIN_STONE_TOOLS, OBTAIN_IRON_TOOLS, OBTAIN_GOLDEN_TOOLS, 
                OBTAIN_DIAMOND_TOOLS, OBTAIN_COPPER_TOOLS
        ));
        
        // Mining
        categories.put("Mining", List.of(
                MINE_DIAMOND_ORE, MINE_EMERALD_ORE, MINE_MOB_SPAWNER, MINE_TURTLE_EGG
        ));
        
        // Dimensions
        categories.put("Dimensions", List.of(
                ENTER_NETHER, ENTER_END
        ));
        
        // Armor
        categories.put("Armor", List.of(
                FILL_ARMOR_STAND, WEAR_LEATHER_ARMOR, WEAR_GOLDEN_ARMOR, WEAR_DIAMOND_ARMOR, 
                WEAR_COPPER_ARMOR, WEAR_IRON_ARMOR, WEAR_CHAIN_ARMOR_PIECE, WEAR_COLORED_LEATHER_ARMOR_PIECE,
                WEAR_CARVED_PUMPKIN_FOR_5_MINUTES, WEAR_UNIQUE_COLORED_LEATHER_ARMOR, WEAR_FULL_ENCHANTED_ARMOR
        ));
        
        // Taming
        categories.put("Taming", List.of(
                TAME_CAT, TAME_PARROT, TAME_HORSE, TAME_WOLF
        ));
        
        // Breeding
        categories.put("Breeding", List.of(
                BREED_4_UNIQUE_ANIMALS, BREED_6_UNIQUE_ANIMALS, BREED_8_UNIQUE_ANIMALS,
                BREED_CHICKEN, BREED_COW, BREED_PIG, BREED_SHEEP, BREED_RABBIT, BREED_HOGLIN, 
                BREED_FOX, BREED_GOAT, BREED_STRIDER, BREED_FROGS
        ));
        
        // Killing
        categories.put("Killing", List.of(
                KILL_WITCH, KILL_ZOMBIE_VILLAGER, KILL_STRAY, KILL_ZOGLIN, KILL_SILVERFISH, 
                KILL_GUARDIAN, KILL_GHAST, KILL_BAT, KILL_SNOW_GOLEM, KILL_SNOW_GOLEM_IN_NETHER, 
                KILL_ELDER_GUARDIAN, KILL_COLORED_SHEEP, KILL_7_UNIQUE_HOSTILE_MOBS, 
                KILL_10_UNIQUE_HOSTILE_MOBS, KILL_13_UNIQUE_HOSTILE_MOBS, KILL_15_UNIQUE_HOSTILE_MOBS,
                KILL_30_UNDEAD_MOBS, KILL_20_ARTHROPOD_MOBS, KILL_100_MOBS, KILL_ALL_RAID_MOBS,
                KILL_BREEZE_USING_WIND_CHARGE, KILL_BLAZE_WITH_SNOWBALL
        ));
        
        // Obtaining Items
        categories.put("Obtaining Items", List.of(
                OBTAIN_RED_NETHER_BRICK_STAIRS, OBTAIN_TROPICAL_FISH_BUCKET, OBTAIN_BOOKSHELF, 
                OBTAIN_MOSSY_STONE_BRICK_WALL, OBTAIN_FLOWERING_AZALEA, OBTAIN_SCAFFOLDING, 
                PLACE_END_CRYSTAL, OBTAIN_BELL, OBTAIN_ENCHANT_BOTTLE, OBTAIN_POWDER_SNOW_BUCKET, 
                OBTAIN_SOUL_LANTERN, OBTAIN_ANCIENT_DEBRIS, OBTAIN_ENDER_CHEST, OBTAIN_HEART_OF_THE_SEA, 
                OBTAIN_WITHER_SKELETON_SKULL, OBTAIN_END_ROD, OBTAIN_SPONGE, OBTAIN_DRAGON_EGG, 
                OBTAIN_TNT, OBTAIN_COBWEB, OBTAIN_MUD_BRICK_WALL, OBTAIN_DAYLIGHT_DETECTOR, 
                OBTAIN_REDSTONE_REPEATER, OBTAIN_REDSTONE_COMPARATOR, OBTAIN_OBSERVER, 
                OBTAIN_ACTIVATOR_RAIL, OBTAIN_DETECTOR_RAIL, OBTAIN_POWERED_RAIL, OBTAIN_DISPENSER, 
                OBTAIN_PISTON, OBTAIN_ALL_RAW_ORE_BLOCKS, OBTAIN_ALL_HORSE_ARMOR, OBTAIN_4_UNIQUE_SEEDS, 
                OBTAIN_6_UNIQUE_FLOWERS, OBTAIN_COLORED_GLAZED_TERRACOTTA, OBTAIN_64_COLORED_WOOL, 
                OBTAIN_64_COLORED_CONCRETE, OBTAIN_WRITTEN_BOOK, FILL_INVENTORY_UNIQUE_ITEMS, 
                OBTAIN_4_UNIQUE_SAPLINGS, OBTAIN_CLOCK, OBTAIN_6_UNIQUE_BUCKETS, OBTAIN_ALL_MINECARTS, 
                OBTAIN_ALL_MUSHROOMS, OBTAIN_7_UNIQUE_WORKSTATIONS, OBTAIN_REDSTONE_LAMP, 
                OBTAIN_SOUL_CAMPFIRE, OBTAIN_ALL_PUMPKINS, OBTAIN_BRICK_WALL, OBTAIN_64_ARROWS,
                OBTAIN_RESIN_BRICK_STAIR
        ));
        
        // Brewing
        categories.put("Brewing", List.of(
                USE_BREWING_STAND, BREW_HEALING_POTION, BREW_INVISIBILITY_POTION, BREW_POISON_POTION, 
                BREW_SWIFTNESS_POTION, BREW_WATER_BREATHING_POTION, BREW_WEAKNESS_POTION, 
                BREW_LINGERING_POTION
        ));
        
        // Biomes
        categories.put("Biomes", List.of(
                VISIT_ICE_SPIKES_BIOME, VISIT_BADLANDS_BIOME, VISIT_MUSHROOM_BIOME
        ));
        
        // Eating/Drinking
        categories.put("Eating/Drinking", List.of(
                EAT_5_UNIQUE_FOOD, EAT_10_UNIQUE_FOOD, EAT_15_UNIQUE_FOOD, EAT_20_UNIQUE_FOOD, 
                EAT_25_UNIQUE_FOOD, EAT_CHORUS_FRUIT, EAT_COOKIE, EAT_GLOW_BERRY, EAT_POISONOUS_POTATO, 
                EAT_PUMPKIN_PIE, EAT_RABBIT_STEW, EAT_SUSPICIOUS_STEW, DRINK_HONEY_BOTTLE, 
                DRINK_WATER_BOTTLE, EAT_CAKE
        ));
        
        // Misc Actions
        categories.put("Misc Actions", List.of(
                TOOT_GOAT_HORN, EMPTY_HUNGER_BAR, REACH_HEIGHT_LIMIT, REACH_BEDROCK, 
                ENRAGE_ZOMBIFIED_PIGLIN, REACH_NETHER_ROOF, SPRINT_1_KM, PUT_BANNER_ON_SHIELD, 
                HAVE_YOUR_SHIELD_DISABLED, ITEM_FRAME_IN_ITEM_FRAME, FILL_CAMPFIRE, 
                PUT_FLOWER_IN_POT, FILL_CHISELED_BOOKSHELF, USE_BRUSH_ON_SUSPICIOUS_BLOCK, 
                SHOOT_FIREWORK_FROM_CROSSBOW, MINE_CRAFTER, LIGHT_CANDLE, PUT_WOLF_ARMOR_ON_WOLF, 
                FILL_BUNDLE, CONSTRUCT_COPPER_GOLEM, FILL_SHELF
        ));
        
        // Advancements
        categories.put("Advancements", List.of(
                GET_THIS_BOAT_HAS_LEGS_ADVANCEMENT, GET_ANY_SPYGLASS_ADVANCEMENT, GET_BULLSEYE_ADVANCEMENT, 
                GET_HOT_TOURIST_DESTINATIONS_ADVANCEMENT, GET_NOT_QUITE_NINE_LIVES_ADVANCEMENT, 
                GET_OH_SHINY_ADVANCEMENT, GET_SNIPER_DUEL_ADVANCEMENT, GET_WHAT_A_DEAL_ADVANCEMENT, 
                GET_HIRED_HELP_ADVANCEMENT, GET_STAY_HYDRATED_ADVANCEMENT, GET_WAX_ON_ADVANCEMENT, 
                GET_WAX_OFF_ADVANCEMENT, GET_COUNTRY_LODE_TAKE_ME_HOME_ADVANCEMENT, 
                GET_HEART_TRANSPLANTER_ADVANCEMENT, GET_10_ADVANCEMENTS, GET_20_ADVANCEMENTS, 
                GET_30_ADVANCEMENTS, GET_A_TERRIBLE_FORTRESS_ADVANCEMENT, 
                GET_THE_CITY_AT_THE_END_OF_THE_GAME_ADVANCEMENT, GET_EYE_SPY_ADVANCEMENT, 
                GET_THOSE_WERE_THE_DAYS_ADVANCEMENT
        ));
        
        // Status Effects
        categories.put("Status Effects", List.of(
                GET_ABSORPTION_STATUS_EFFECT, GET_BAD_OMEN_STATUS_EFFECT, GET_GLOWING_STATUS_EFFECT, 
                GET_JUMP_BOOST_STATUS_EFFECT, GET_LEVITATION_STATUS_EFFECT, GET_MINING_FATIGUE_STATUS_EFFECT, 
                GET_NAUSEA_STATUS_EFFECT, GET_POISON_STATUS_EFFECT, GET_WEAKNESS_STATUS_EFFECT, 
                REMOVE_STATUS_EFFECT_USING_MILK, GET_3_STATUS_EFFECTS_AT_ONCE, GET_4_STATUS_EFFECTS_AT_ONCE, 
                GET_6_STATUS_EFFECTS_AT_ONCE
        ));
        
        // Experience
        categories.put("Experience", List.of(
                REACH_EXP_LEVEL_15, REACH_EXP_LEVEL_30
        ));
        
        // Riding
        categories.put("Riding", List.of(
                RIDE_HORSE, RIDE_PIG, RIDE_MINECART
        ));
        
        // Workstations
        categories.put("Workstations", List.of(
                USE_STONECUTTER, USE_ANVIL, USE_ENCHANTING_TABLE, USE_GRINDSTONE, USE_LOOM, 
                USE_SMITHING_TABLE, USE_CAULDRON, USE_COMPOSTER, USE_JUKEBOX, USE_GLOW_INK
        ));
        
        // Death/Damage
        categories.put("Death/Damage", List.of(
                DIE_BY_ANVIL, DIE_BY_BEE_STING, DIE_BY_BERRY_BUSH, DIE_BY_CACTUS, 
                DIE_BY_FALLING_OFF_VINE, DIE_BY_FALLING_STALACTITE, DIE_BY_FIREWORK, 
                DIE_BY_INTENTIONAL_GAME_DESIGN, DIE_BY_IRON_GOLEM, DIE_TO_POLAR_BEAR, 
                DIE_BY_MAGIC, DIE_BY_TNT_MINECART, FREEZE_TO_DEATH, TAKE_200_DAMAGE, 
                DEAL_400_DAMAGE
        ));
        
        // Team Goals
        categories.put("Team Goals", List.of(
                KILL_OTHER_PLAYER, OPPONENT_OBTAINS_CRAFTING_TABLE, OPPONENT_OBTAINS_OBSIDIAN, 
                OPPONENT_OBTAINS_SEEDS, OPPONENT_CATCHES_ON_FIRE, OPPONENT_DIES_3_TIMES, 
                OPPONENT_DIES, OPPONENT_HIT_BY_EGG, OPPONENT_HIT_BY_SNOWBALL, 
                OPPONENT_TAKES_100_DAMAGE, OPPONENT_TAKES_FALL_DAMAGE, OPPONENT_TOUCHES_WATER, 
                OPPONENT_EATS_FOOD, HAVE_MORE_XP_LEVELS, HAVE_MORE_UNIQUE_CRAFTS, 
                HAVE_MOST_PLAYER_KILLS, HAVE_MOST_ADVANCEMENTS, HAVE_MOST_HOPPERS
        ));
        
        return categories;
    }
}
