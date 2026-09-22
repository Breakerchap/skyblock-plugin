package io.github.breakerchap.skyblock.advancement;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdvancementCatalog {
    private static final List<Material> COLLECTION_MATERIALS = List.of(
        Material.ANDESITE, Material.DIORITE, Material.GRANITE, Material.DEEPSLATE,
        Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK, Material.SAND,
        Material.RED_SAND, Material.CLAY_BALL, Material.BRICK, Material.TERRACOTTA,
        Material.SNOWBALL, Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
        Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.PRISMARINE_SHARD,
        Material.PRISMARINE_CRYSTALS, Material.SPONGE, Material.SEA_LANTERN,
        Material.AMETHYST_SHARD, Material.COPPER_INGOT, Material.GOLD_INGOT,
        Material.LAPIS_LAZULI, Material.REDSTONE, Material.COAL, Material.FLINT,
        Material.STRING, Material.BONE, Material.GUNPOWDER, Material.SLIME_BALL,
        Material.ENDER_PEARL, Material.BLAZE_POWDER, Material.GHAST_TEAR,
        Material.MAGMA_CREAM, Material.PHANTOM_MEMBRANE, Material.LEATHER,
        Material.FEATHER, Material.RABBIT_HIDE, Material.INK_SAC, Material.GLOW_INK_SAC,
        Material.NAUTILUS_SHELL, Material.TURTLE_SCUTE, Material.TURTLE_EGG,
        Material.HONEYCOMB, Material.HONEY_BOTTLE, Material.APPLE, Material.GOLDEN_APPLE,
        Material.COCOA_BEANS, Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT,
        Material.SHULKER_SHELL, Material.ELYTRA, Material.DRAGON_BREATH,
        Material.NETHER_STAR, Material.TOTEM_OF_UNDYING, Material.TRIDENT,
        Material.HEART_OF_THE_SEA, Material.SADDLE, Material.NAME_TAG,
        Material.EXPERIENCE_BOTTLE, Material.MUSIC_DISC_13, Material.GOAT_HORN,
        Material.SNIFFER_EGG, Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD,
        Material.ARMADILLO_SCUTE, Material.BREEZE_ROD, Material.HEAVY_CORE,
        Material.OMINOUS_BOTTLE, Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY
    );

    private AdvancementCatalog() {
    }

    public static List<AdvancementDefinition> definitions() {
        List<AdvancementDefinition> d = new ArrayList<>();

        root(d, "root", "Skybound", "Build a world out of almost nothing.", Material.GRASS_BLOCK);
        root(d, "farming/root", "Life Finds a Way", "Turn a few living things into an ecosystem.", Material.WHEAT);
        root(d, "engineering/root", "Industry", "Make the void increasingly over-engineered.", Material.PISTON);
        root(d, "combat/root", "Things Fight Back", "Apparently the void was not empty enough.", Material.IRON_SWORD);
        root(d, "exploration/root", "Horizons", "There are stranger things beyond your island.", Material.SPYGLASS);
        root(d, "community/root", "Together", "Server-wide milestones everyone contributes to.", Material.BELL);
        root(d, "collection/root", "Museum of Stuff", "Collect things the void really did not want you to have.", Material.BUNDLE);
        root(d, "oddities/root", "Questionable Decisions", "For achievements nobody sensible would plan.", Material.POISONOUS_POTATO);

        a(d, "getting_started/cobblestone", "root", "One Block at a Time", "Obtain cobblestone.", Material.COBBLESTONE);
        a(d, "getting_started/stone", "getting_started/cobblestone", "Slightly Fancier Rock", "Obtain stone.", Material.STONE);
        a(d, "getting_started/tree", "root", "Arborist", "Grow a tree.", Material.OAK_SAPLING);
        a(d, "getting_started/crafting", "root", "The Table", "Make a crafting table.", Material.CRAFTING_TABLE);
        a(d, "getting_started/furnace", "getting_started/crafting", "Hot Box", "Make a furnace.", Material.FURNACE);
        a(d, "getting_started/charcoal", "getting_started/furnace", "Artificial Coal", "Make charcoal.", Material.CHARCOAL);
        goal(d, "getting_started/iron", "getting_started/cobblestone", "Industry Begins", "Obtain an iron ingot.", Material.IRON_INGOT);
        a(d, "getting_started/bucket", "getting_started/iron", "Portable Fluid", "Make a bucket.", Material.BUCKET);
        goal(d, "getting_started/lava", "getting_started/bucket", "Hot Property", "Obtain a lava bucket.", Material.LAVA_BUCKET);
        a(d, "getting_started/water", "getting_started/bucket", "Luxury Hydration", "Obtain a water bucket.", Material.WATER_BUCKET);
        a(d, "getting_started/glass", "getting_started/furnace", "Windows to Nowhere", "Obtain glass.", Material.GLASS);
        a(d, "getting_started/bed", "getting_started/tree", "Home, Technically", "Make a bed.", Material.WHITE_BED);
        a(d, "getting_started/chest", "getting_started/crafting", "Storage Problem", "Make a chest.", Material.CHEST);
        a(d, "getting_started/torch", "getting_started/charcoal", "Let There Be Light", "Make a torch.", Material.TORCH);
        a(d, "getting_started/shield", "getting_started/iron", "Not Today", "Make a shield.", Material.SHIELD);
        goal(d, "getting_started/diamond", "getting_started/iron", "Something Shiny", "Obtain a diamond.", Material.DIAMOND);
        goal(d, "getting_started/enchanting", "getting_started/diamond", "Magic Table", "Make an enchanting table.", Material.ENCHANTING_TABLE);
        a(d, "getting_started/emerald", "getting_started/iron", "Green Money", "Obtain an emerald.", Material.EMERALD);
        goal(d, "civilisation/villager", "getting_started/iron", "Civilisation", "Breed two villagers.", Material.EMERALD);

        a(d, "farming/wheat", "farming/root", "Bread Begins", "Obtain wheat.", Material.WHEAT);
        a(d, "farming/carrot", "farming/root", "Orange Acquisition", "Obtain a carrot.", Material.CARROT);
        a(d, "farming/potato", "farming/root", "Boil 'Em, Mash 'Em", "Obtain a potato.", Material.POTATO);
        a(d, "farming/beetroot", "farming/root", "Red Roots", "Obtain beetroot.", Material.BEETROOT);
        a(d, "farming/pumpkin", "farming/root", "Pumpkin Patch", "Obtain a pumpkin.", Material.PUMPKIN);
        a(d, "farming/melon", "farming/root", "Melon Somewhere", "Obtain a melon slice.", Material.MELON_SLICE);
        a(d, "farming/sugar_cane", "farming/root", "Paperwork", "Obtain sugar cane.", Material.SUGAR_CANE);
        a(d, "farming/cactus", "farming/root", "Do Not Hug", "Obtain cactus.", Material.CACTUS);
        a(d, "farming/bamboo", "farming/root", "Suspiciously Fast Plant", "Obtain bamboo.", Material.BAMBOO);
        a(d, "farming/cocoa", "farming/root", "Chocolate Infrastructure", "Obtain cocoa beans.", Material.COCOA_BEANS);
        a(d, "farming/kelp", "farming/root", "Oceanless Kelp", "Obtain kelp.", Material.KELP);
        a(d, "farming/red_mushroom", "farming/root", "Red Cap", "Obtain a red mushroom.", Material.RED_MUSHROOM);
        a(d, "farming/brown_mushroom", "farming/root", "Brown Cap", "Obtain a brown mushroom.", Material.BROWN_MUSHROOM);
        a(d, "farming/glow_berries", "farming/root", "Mood Lighting", "Obtain glow berries.", Material.GLOW_BERRIES);
        a(d, "farming/sweet_berries", "farming/root", "Prickly Snack", "Obtain sweet berries.", Material.SWEET_BERRIES);
        a(d, "farming/moss", "farming/root", "Green Carpet", "Obtain a moss block.", Material.MOSS_BLOCK);
        a(d, "farming/dripstone", "farming/root", "Pointy Agriculture", "Obtain pointed dripstone.", Material.POINTED_DRIPSTONE);
        a(d, "farming/honey", "farming/root", "Bee Product", "Obtain a honey bottle.", Material.HONEY_BOTTLE);
        a(d, "farming/egg", "farming/root", "Which Came First?", "Obtain an egg.", Material.EGG);
        a(d, "farming/wool", "farming/root", "Cloud Farming", "Obtain wool in a world made mostly of sky.", Material.WHITE_WOOL);
        goal(d, "farming/breed", "farming/root", "Population Growth", "Breed a creature.", Material.WHEAT);
        goal(d, "farming/breed_10", "farming/breed", "Small Farm", "Breed 10 creatures.", Material.HAY_BLOCK);
        challenge(d, "farming/breed_100", "farming/breed_10", "Industrial Romance", "Breed 100 creatures.", Material.GOLDEN_CARROT);
        goal(d, "farming/trees_10", "farming/root", "Mini Forest", "Grow 10 trees.", Material.OAK_LOG);
        challenge(d, "farming/trees_100", "farming/trees_10", "Deforestation Somehow", "Grow 100 trees in the void.", Material.OAK_LEAVES);
        a(d, "farming/bread", "farming/wheat", "Actual Food", "Bake bread.", Material.BREAD);
        goal(d, "farming/cake", "farming/root", "Let Them Eat Cake", "Make cake.", Material.CAKE);
        goal(d, "farming/goat_boat", "farming/root", "Whatever Floats Your Goat", "Put a goat in a boat.", Material.OAK_BOAT);
        goal(d, "farming/bee_boat", "farming/root", "Buzz Cruise", "Put a bee in a boat.", Material.BEE_NEST);
        goal(d, "farming/goat_breeder", "farming/root", "The Kids Are Alright", "Breed two goats in the sky.", Material.GOAT_HORN);
        challenge(d, "farming/village_people", "farming/root", "Village People", "Have at least five villagers together.", Material.BELL);

        a(d, "engineering/redstone", "engineering/root", "Power", "Obtain redstone dust.", Material.REDSTONE);
        goal(d, "engineering/void_trowel", "engineering/root", "Bedrock at Home", "Craft and use a Void Trowel.", Material.BRUSH);
        goal(d, "engineering/bridge_64", "engineering/void_trowel", "Don't Look Down", "Place 64 bridge blocks with a Void Trowel.", Material.COBBLESTONE);
        challenge(d, "engineering/bridge_500", "engineering/bridge_64", "Civil Engineering", "Place 500 bridge blocks with a Void Trowel.", Material.STONE_BRICKS);
        goal(d, "engineering/speed_bridge", "engineering/void_trowel", "Bedrock Bridger", "Place a Void Trowel block while sprinting.", Material.SCAFFOLDING);
        goal(d, "engineering/wayfarer_call", "engineering/root", "Someone Actually Came", "Summon a wandering trader with the Wayfarer's Bell.", Material.LEAD);
        goal(d, "engineering/trader_5", "engineering/wayfarer_call", "Frequent Caller", "Summon five wandering traders.", Material.EMERALD);
        challenge(d, "engineering/trader_25", "engineering/trader_5", "He Knows Your Number", "Summon 25 wandering traders.", Material.BELL);
        challenge(d, "engineering/beacon", "engineering/root", "Visible From Everywhere", "Place a beacon.", Material.BEACON);
        a(d, "engineering/cobble_64", "engineering/root", "A Stack of Progress", "Mine 64 cobblestone.", Material.COBBLESTONE);
        goal(d, "engineering/cobble_1000", "engineering/cobble_64", "Rock Collection", "Mine 1,000 cobblestone.", Material.COBBLESTONE);
        challenge(d, "engineering/cobble_10000", "engineering/cobble_1000", "Geology Degree", "Mine 10,000 cobblestone.", Material.COBBLESTONE);
        a(d, "engineering/place_100", "engineering/root", "Expansion Pack", "Place 100 blocks.", Material.BRICKS);
        goal(d, "engineering/place_1000", "engineering/place_100", "Urban Planning", "Place 1,000 blocks.", Material.STONE_BRICKS);
        goal(d, "engineering/wayfarer_bell", "engineering/root", "Call Me Maybe", "Craft the Wayfarer's Bell.", Material.BELL);

        a(d, "combat/zombie", "combat/root", "Unwelcome Guest", "Kill a zombie.", Material.ZOMBIE_HEAD);
        a(d, "combat/skeleton", "combat/root", "Bone Problem", "Kill a skeleton.", Material.BONE);
        a(d, "combat/creeper", "combat/root", "Property Damage Prevention", "Kill a creeper.", Material.GUNPOWDER);
        a(d, "combat/spider", "combat/root", "Eight Legs Too Many", "Kill a spider.", Material.SPIDER_EYE);
        a(d, "combat/witch", "combat/root", "Return to Sender", "Kill a witch.", Material.GLASS_BOTTLE);
        a(d, "combat/slime", "combat/root", "Elastic Violence", "Kill a slime.", Material.SLIME_BALL);
        a(d, "combat/enderman", "combat/root", "Do Not Make Eye Contact", "Kill an enderman.", Material.ENDER_PEARL);
        a(d, "combat/drowned", "combat/root", "Where Did You Even Come From?", "Kill a drowned.", Material.TRIDENT);
        a(d, "combat/phantom", "combat/root", "Insomnia Tax", "Kill a phantom.", Material.PHANTOM_MEMBRANE);
        a(d, "combat/blaze", "combat/root", "Firefight", "Kill a blaze.", Material.BLAZE_ROD);
        a(d, "combat/ghast", "combat/root", "Return to Ghast", "Kill a ghast.", Material.GHAST_TEAR);
        a(d, "combat/magma_cube", "combat/root", "Hot Slime", "Kill a magma cube.", Material.MAGMA_CREAM);
        a(d, "combat/wither_skeleton", "combat/root", "Tall Skeleton", "Kill a wither skeleton.", Material.WITHER_SKELETON_SKULL);
        a(d, "combat/guardian", "combat/root", "Oceanless Guardian", "Kill a guardian.", Material.PRISMARINE_SHARD);
        a(d, "combat/shulker", "combat/root", "Boxing Match", "Kill a shulker.", Material.SHULKER_SHELL);
        a(d, "combat/piglin_brute", "combat/root", "No Negotiating", "Kill a piglin brute.", Material.GOLDEN_AXE);
        challenge(d, "combat/wither", "combat/root", "Three Heads Are Worse Than One", "Kill the Wither.", Material.NETHER_STAR);
        challenge(d, "combat/dragon", "combat/root", "Nothing Underneath", "Defeat the Ender Dragon.", Material.DRAGON_HEAD);
        a(d, "combat/kills_10", "combat/root", "Self Defence", "Kill 10 hostile mobs.", Material.STONE_SWORD);
        goal(d, "combat/kills_100", "combat/kills_10", "Pest Control", "Kill 100 hostile mobs.", Material.IRON_SWORD);
        challenge(d, "combat/kills_1000", "combat/kills_100", "Population Control", "Kill 1,000 hostile mobs.", Material.DIAMOND_SWORD);
        goal(d, "combat/skeleton_crew", "combat/root", "Skeleton Crew", "Kill a skeleton while it is riding in a boat.", Material.OAK_BOAT);
        goal(d, "combat/air_superiority", "combat/root", "Air Superiority", "Kill a hostile mob while there is no block beneath you.", Material.ELYTRA);

        goal(d, "nether/root", "root", "Beyond the Void", "Enter the Nether.", Material.OBSIDIAN);
        a(d, "nether/quartz", "nether/root", "White Rock", "Obtain Nether quartz.", Material.QUARTZ);
        a(d, "nether/glowstone", "nether/root", "Portable Sun", "Obtain glowstone dust.", Material.GLOWSTONE_DUST);
        a(d, "nether/soul_sand", "nether/root", "Uncomfortable Beach", "Obtain soul sand.", Material.SOUL_SAND);
        a(d, "nether/nether_wart", "nether/root", "Infernal Gardening", "Obtain Nether wart.", Material.NETHER_WART);
        goal(d, "nether/blaze", "nether/root", "Firepower", "Obtain a blaze rod.", Material.BLAZE_ROD);
        a(d, "nether/magma_cream", "nether/root", "Cream, Technically", "Obtain magma cream.", Material.MAGMA_CREAM);
        a(d, "nether/crying_obsidian", "nether/root", "Sad Rock", "Obtain crying obsidian.", Material.CRYING_OBSIDIAN);
        goal(d, "nether/ancient_debris", "nether/root", "Old Rubbish", "Obtain ancient debris.", Material.ANCIENT_DEBRIS);
        a(d, "nether/netherite_scrap", "nether/ancient_debris", "Scrap Metal", "Obtain netherite scrap.", Material.NETHERITE_SCRAP);
        challenge(d, "nether/netherite", "nether/netherite_scrap", "Overengineered", "Obtain a netherite ingot.", Material.NETHERITE_INGOT);
        goal(d, "nether/respawn_anchor", "nether/root", "A Bed Would Be Too Easy", "Make a respawn anchor.", Material.RESPAWN_ANCHOR);
        goal(d, "nether/wither_skull", "nether/root", "Bad Decoration", "Obtain a wither skeleton skull.", Material.WITHER_SKELETON_SKULL);
        challenge(d, "nether/nether_star", "nether/wither_skull", "Star From Hell", "Obtain a Nether star.", Material.NETHER_STAR);

        goal(d, "nether/bed_attempt", "nether/root", "What Did You Expect?", "Try to sleep in the Nether.", Material.RED_BED);

        goal(d, "end/root", "nether/blaze", "The Last Horizon", "Enter the End.", Material.END_STONE);
        a(d, "end/end_stone", "end/root", "Moon Rock", "Obtain end stone.", Material.END_STONE);
        a(d, "end/chorus", "end/root", "Alien Fruit", "Obtain chorus fruit.", Material.CHORUS_FRUIT);
        a(d, "end/purpur", "end/root", "Purple Architecture", "Obtain purpur.", Material.PURPUR_BLOCK);
        a(d, "end/end_rod", "end/root", "End Lighting", "Obtain an end rod.", Material.END_ROD);
        goal(d, "end/shulker_shell", "end/root", "Portable Box Parts", "Obtain a shulker shell.", Material.SHULKER_SHELL);
        goal(d, "end/shulker_box", "end/shulker_shell", "Inventory Expansion", "Make a shulker box.", Material.SHULKER_BOX);
        challenge(d, "end/elytra", "end/root", "Void Insurance", "Obtain an elytra.", Material.ELYTRA);
        a(d, "end/dragon_head", "end/root", "Trophy Head", "Obtain a dragon head.", Material.DRAGON_HEAD);
        a(d, "end/dragon_breath", "end/root", "Bottle the Boss", "Obtain dragon's breath.", Material.DRAGON_BREATH);
        challenge(d, "end/dragon", "end/root", "The End, Again", "Defeat the Ender Dragon.", Material.DRAGON_EGG);
        goal(d, "end/end_crystal", "end/root", "Bad Idea in Glass", "Make an end crystal.", Material.END_CRYSTAL);
        challenge(d, "end/beacon", "end/dragon", "A Light in the Void", "Place a beacon.", Material.BEACON);
        goal(d, "end/void_death", "end/root", "The Void Has Layers", "Fall into the void in the End.", Material.ENDER_PEARL);

        hiddenGoal(d, "exploration/lush", "exploration/root", "A Speck of Green", "Find the Lush Outcrop.", Material.MOSS_BLOCK);
        hiddenGoal(d, "exploration/dripstone", "exploration/root", "Stone Teeth", "Find the Dripstone Spire.", Material.POINTED_DRIPSTONE);
        hiddenGoal(d, "exploration/moor", "exploration/root", "Mud in the Sky", "Find the Witch's Moor.", Material.MUD);
        hiddenGoal(d, "exploration/portal", "exploration/root", "Who Built This?", "Find the Ruined Portal.", Material.CRYING_OBSIDIAN);
        hiddenGoal(d, "exploration/monument", "exploration/root", "Sea Without an Ocean", "Find the Monument Shard.", Material.PRISMARINE);
        hiddenGoal(d, "exploration/desert", "exploration/root", "Dry Patch", "Find the Desert Shrine.", Material.SANDSTONE);
        hiddenGoal(d, "exploration/frozen", "exploration/root", "Cold Front", "Find the Frozen Observatory.", Material.PACKED_ICE);
        hiddenGoal(d, "exploration/mushroom", "exploration/root", "Fungal Real Estate", "Find the Mushroom Colony.", Material.MYCELIUM);
        hiddenGoal(d, "exploration/geode", "exploration/root", "Purple Pocket", "Find the Amethyst Geode.", Material.AMETHYST_BLOCK);
        hiddenGoal(d, "exploration/apiary", "exploration/root", "Buzzing in the Void", "Find the Apiary.", Material.BEEHIVE);
        hiddenChallenge(d, "exploration/end_shrine", "exploration/root", "A Door to Somewhere Else", "Find the End Shrine.", Material.END_PORTAL_FRAME);
        hiddenGoal(d, "exploration/village", "exploration/root", "People?!", "Find the Little Village.", Material.BELL);
        challenge(d, "exploration/all", "exploration/root", "Void Cartographer", "Discover every exploration island.", Material.FILLED_MAP);

        challenge(d, "community/cobble", "community/root", "Stone by Stone", "As a server, mine 5,000 cobblestone.", Material.COBBLESTONE);
        challenge(d, "community/builder", "community/root", "Somewhere to Live", "As a server, place 3,000 blocks.", Material.BRICKS);
        challenge(d, "community/hunter", "community/root", "Night Shift", "As a server, kill 250 hostile mobs.", Material.IRON_SWORD);
        challenge(d, "community/life", "community/root", "It Takes a Village", "As a server, breed 50 creatures.", Material.WHEAT);
        challenge(d, "community/forest", "community/root", "Reforestation", "As a server, grow 250 trees.", Material.OAK_SAPLING);
        challenge(d, "community/fish", "community/root", "Somehow, Fishing", "As a server, catch 100 fish.", Material.COD);
        challenge(d, "community/crafting", "community/root", "Factory Floor", "As a server, craft 5,000 times.", Material.CRAFTING_TABLE);
        challenge(d, "community/harvest", "community/root", "Agricultural Society", "As a server, harvest 2,000 crops.", Material.GOLDEN_HOE);

        a(d, "oddities/rotten_flesh", "oddities/root", "Fine Dining", "Eat rotten flesh.", Material.ROTTEN_FLESH);
        a(d, "oddities/spider_eye", "oddities/root", "Absolutely Not Food", "Eat a spider eye.", Material.SPIDER_EYE);
        a(d, "oddities/poisonous_potato", "oddities/root", "It Says Poisonous", "Eat a poisonous potato anyway.", Material.POISONOUS_POTATO);
        a(d, "oddities/pufferfish", "oddities/root", "Inflation", "Eat a pufferfish.", Material.PUFFERFISH);
        a(d, "oddities/sleep", "oddities/root", "Luxury Accommodation", "Sleep in a bed above the void.", Material.RED_BED);
        goal(d, "oddities/void_death", "oddities/root", "Gravity Still Works", "Die in the void.", Material.FEATHER);
        challenge(d, "oddities/void_deaths_10", "oddities/void_death", "Research Confirmed", "Die in the void 10 times.", Material.SKELETON_SKULL);
        goal(d, "oddities/deaths_10", "oddities/root", "Learning Experience", "Die 10 times.", Material.TOTEM_OF_UNDYING);
        challenge(d, "oddities/deaths_50", "oddities/deaths_10", "Persistent, If Nothing Else", "Die 50 times.", Material.RESPAWN_ANCHOR);
        a(d, "oddities/fish", "oddities/root", "There's Water Down There?", "Catch a fish.", Material.FISHING_ROD);
        goal(d, "oddities/fish_25", "oddities/fish", "Sky Angler", "Catch 25 fish.", Material.COD);
        challenge(d, "oddities/cobble_10000", "oddities/root", "I Could Have Played Normal Minecraft", "Mine 10,000 cobblestone.", Material.STONECUTTER);
        a(d, "oddities/dirt_64", "oddities/root", "Real Estate Mogul", "Carry at least 64 dirt.", Material.DIRT);
        goal(d, "oddities/emerald_64", "oddities/root", "Capitalism", "Carry at least 64 emeralds.", Material.EMERALD_BLOCK);
        a(d, "oddities/bread_64", "oddities/root", "Carb Loading", "Carry at least 64 bread.", Material.BREAD);
        a(d, "oddities/beds_16", "oddities/root", "Landlord", "Carry at least 16 beds.", Material.WHITE_BED);
        a(d, "oddities/torches_64", "oddities/root", "Fear of the Dark", "Carry at least 64 torches.", Material.TORCH);
        a(d, "oddities/cactus_64", "oddities/root", "Touch Grassn't", "Carry at least 64 cactus.", Material.CACTUS);
        a(d, "oddities/eggs_64", "oddities/root", "Don't Put Them All in One Basket", "Carry at least 64 eggs.", Material.EGG);
        a(d, "oddities/flesh_64", "oddities/root", "Emergency Rations", "Carry at least 64 rotten flesh.", Material.ROTTEN_FLESH);
        a(d, "oddities/thermodynamics", "oddities/root", "Thermodynamics", "Carry both a water bucket and a lava bucket.", Material.OBSIDIAN);
        a(d, "oddities/diamond_hoe", "oddities/root", "Commitment to Agriculture", "Make a diamond hoe.", Material.DIAMOND_HOE);
        challenge(d, "oddities/netherite_hoe", "oddities/diamond_hoe", "Terrible Financial Decision", "Make a netherite hoe.", Material.NETHERITE_HOE);
        a(d, "oddities/cookie", "oddities/root", "Balanced Diet", "Make a cookie.", Material.COOKIE);
        a(d, "oddities/boat", "oddities/root", "Optimist", "Make a boat. In a sky world.", Material.OAK_BOAT);
        a(d, "oddities/fishing_rod", "oddities/root", "Sky Fishing", "Make a fishing rod.", Material.FISHING_ROD);
        a(d, "oddities/spyglass", "oddities/root", "Still Nothing", "Make a spyglass and look for land.", Material.SPYGLASS);
        a(d, "oddities/clock", "oddities/root", "Time Passes Up Here Too", "Make a clock.", Material.CLOCK);
        a(d, "oddities/compass", "oddities/root", "This Seems Less Useful", "Make a compass.", Material.COMPASS);
        a(d, "oddities/scaffolding", "oddities/root", "OSHA Has Left the Server", "Obtain scaffolding.", Material.SCAFFOLDING);
        goal(d, "oddities/safety_third", "oddities/root", "Safety Third", "Use the Void Trowel while wearing no armour.", Material.LEATHER_BOOTS);

        for (Material material : COLLECTION_MATERIALS) {
            d.add(new AdvancementDefinition(
                collectionId(material),
                "collection/root",
                pretty(material),
                "Obtain " + pretty(material).toLowerCase() + ".",
                material,
                "task",
                false,
                3
            ));
        }

        return Collections.unmodifiableList(d);
    }

    public static List<Material> collectionMaterials() {
        return COLLECTION_MATERIALS;
    }

    public static String collectionId(Material material) {
        return "collection/" + material.name().toLowerCase();
    }

    public static String pretty(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static void root(List<AdvancementDefinition> d, String id, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, null, title, description, icon, "task", false, 0));
    }

    private static void a(List<AdvancementDefinition> d, String id, String parent, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, parent, title, description, icon, "task", false, 5));
    }

    private static void goal(List<AdvancementDefinition> d, String id, String parent, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, parent, title, description, icon, "goal", false, 12));
    }

    private static void challenge(List<AdvancementDefinition> d, String id, String parent, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, parent, title, description, icon, "challenge", false, 25));
    }

    private static void hiddenGoal(List<AdvancementDefinition> d, String id, String parent, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, parent, title, description, icon, "goal", true, 10));
    }

    private static void hiddenChallenge(List<AdvancementDefinition> d, String id, String parent, String title, String description, Material icon) {
        d.add(new AdvancementDefinition(id, parent, title, description, icon, "challenge", true, 20));
    }
}
