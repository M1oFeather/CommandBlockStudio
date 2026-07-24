package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class ParticleDisplayNames {
    private static final Map<String, String> CHINESE = Map.ofEntries(
            Map.entry("angry_villager", "愤怒村民"),
            Map.entry("block", "方块碎屑"),
            Map.entry("block_marker", "方块标记"),
            Map.entry("bubble", "气泡"),
            Map.entry("cloud", "云雾"),
            Map.entry("crit", "暴击"),
            Map.entry("damage_indicator", "伤害指示"),
            Map.entry("dragon_breath", "龙息"),
            Map.entry("dripping_lava", "滴落熔岩"),
            Map.entry("falling_lava", "坠落熔岩"),
            Map.entry("landing_lava", "落地熔岩"),
            Map.entry("dripping_water", "滴落水滴"),
            Map.entry("falling_water", "坠落水滴"),
            Map.entry("dust", "红石粉尘"),
            Map.entry("dust_color_transition", "渐变粉尘"),
            Map.entry("effect", "药水效果"),
            Map.entry("elder_guardian", "远古守卫者幻影"),
            Map.entry("enchanted_hit", "附魔攻击"),
            Map.entry("enchant", "附魔符文"),
            Map.entry("end_rod", "末地烛"),
            Map.entry("entity_effect", "实体药水效果"),
            Map.entry("explosion_emitter", "大型爆炸发射器"),
            Map.entry("explosion", "爆炸"),
            Map.entry("gust", "风爆"),
            Map.entry("small_gust", "小型风爆"),
            Map.entry("gust_emitter_large", "大型风爆发射器"),
            Map.entry("gust_emitter_small", "小型风爆发射器"),
            Map.entry("sonic_boom", "音波冲击"),
            Map.entry("falling_dust", "坠落粉尘"),
            Map.entry("firework", "烟花火星"),
            Map.entry("fishing", "钓鱼水花"),
            Map.entry("flame", "火焰"),
            Map.entry("infested", "虫蚀"),
            Map.entry("cherry_leaves", "樱花落叶"),
            Map.entry("sculk_soul", "幽匿灵魂"),
            Map.entry("sculk_charge", "幽匿充能"),
            Map.entry("sculk_charge_pop", "幽匿充能爆裂"),
            Map.entry("soul_fire_flame", "灵魂火焰"),
            Map.entry("soul", "灵魂"),
            Map.entry("flash", "闪光"),
            Map.entry("happy_villager", "开心村民"),
            Map.entry("composter", "堆肥"),
            Map.entry("heart", "爱心"),
            Map.entry("instant_effect", "瞬间药水效果"),
            Map.entry("item", "物品碎屑"),
            Map.entry("vibration", "振动"),
            Map.entry("item_slime", "史莱姆球碎屑"),
            Map.entry("item_cobweb", "蜘蛛网碎屑"),
            Map.entry("item_snowball", "雪球碎屑"),
            Map.entry("large_smoke", "大型烟雾"),
            Map.entry("lava", "熔岩火花"),
            Map.entry("mycelium", "菌丝"),
            Map.entry("note", "音符"),
            Map.entry("poof", "消散烟雾"),
            Map.entry("portal", "传送门"),
            Map.entry("rain", "雨滴"),
            Map.entry("smoke", "烟雾"),
            Map.entry("white_smoke", "白色烟雾"),
            Map.entry("sneeze", "喷嚏"),
            Map.entry("spit", "羊驼唾沫"),
            Map.entry("squid_ink", "鱿鱼墨汁"),
            Map.entry("sweep_attack", "横扫攻击"),
            Map.entry("totem_of_undying", "不死图腾"),
            Map.entry("underwater", "水下悬浮物"),
            Map.entry("splash", "水花"),
            Map.entry("witch", "女巫魔法"),
            Map.entry("bubble_pop", "气泡破裂"),
            Map.entry("current_down", "向下涡流"),
            Map.entry("bubble_column_up", "上升气泡柱"),
            Map.entry("nautilus", "鹦鹉螺"),
            Map.entry("dolphin", "海豚恩惠"),
            Map.entry("campfire_cosy_smoke", "营火烟雾"),
            Map.entry("campfire_signal_smoke", "营火信号烟雾"),
            Map.entry("dripping_honey", "滴落蜂蜜"),
            Map.entry("falling_honey", "坠落蜂蜜"),
            Map.entry("landing_honey", "落地蜂蜜"),
            Map.entry("falling_nectar", "坠落花蜜"),
            Map.entry("falling_spore_blossom", "坠落孢子花"),
            Map.entry("ash", "灰烬"),
            Map.entry("crimson_spore", "绯红孢子"),
            Map.entry("warped_spore", "诡异孢子"),
            Map.entry("spore_blossom_air", "孢子花空气"),
            Map.entry("dripping_obsidian_tear", "滴落黑曜石泪"),
            Map.entry("falling_obsidian_tear", "坠落黑曜石泪"),
            Map.entry("landing_obsidian_tear", "落地黑曜石泪"),
            Map.entry("reverse_portal", "反向传送门"),
            Map.entry("white_ash", "白色灰烬"),
            Map.entry("small_flame", "小型火焰"),
            Map.entry("snowflake", "雪花"),
            Map.entry("dripping_dripstone_lava", "滴水石锥熔岩滴"),
            Map.entry("falling_dripstone_lava", "滴水石锥坠落熔岩"),
            Map.entry("dripping_dripstone_water", "滴水石锥水滴"),
            Map.entry("falling_dripstone_water", "滴水石锥坠落水滴"),
            Map.entry("glow_squid_ink", "发光鱿鱼墨汁"),
            Map.entry("glow", "荧光"),
            Map.entry("wax_on", "涂蜡"),
            Map.entry("wax_off", "除蜡"),
            Map.entry("electric_spark", "电火花"),
            Map.entry("scrape", "刮削氧化层"),
            Map.entry("shriek", "尖啸"),
            Map.entry("egg_crack", "嗅探兽蛋裂纹"),
            Map.entry("dust_plume", "尘土羽流"),
            Map.entry("trial_spawner_detection", "试炼刷怪笼侦测"),
            Map.entry("trial_spawner_detection_ominous", "不祥试炼刷怪笼侦测"),
            Map.entry("vault_connection", "宝库连接"),
            Map.entry("dust_pillar", "粉尘柱"),
            Map.entry("ominous_spawning", "不祥生成"),
            Map.entry("raid_omen", "袭击之兆"),
            Map.entry("trial_omen", "试炼之兆")
    );

    private ParticleDisplayNames() {
    }

    public static String get(Identifier id) {
        if (CommandBlockStudio.useChineseCommandInsight()) {
            String translated = "minecraft".equals(id.getNamespace()) ? CHINESE.get(id.getPath()) : null;
            if (translated != null) {
                return translated;
            }
        }
        return humanize(id.getPath());
    }

    private static String humanize(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
