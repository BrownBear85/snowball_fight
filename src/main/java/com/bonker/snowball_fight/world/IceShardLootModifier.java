package com.bonker.snowball_fight.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class IceShardLootModifier extends LootModifier {
    public static final Codec<IceShardLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).and(
            inst.group(
                    ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
                    Codec.INT.fieldOf("maxRoll").forGetter(m -> m.maxRoll)
            )).apply(inst, IceShardLootModifier::new)
    );

    private final Item item;
    private final int maxRoll;

    public IceShardLootModifier(LootItemCondition[] conditionsIn, Item item, int maxRoll) {
        super(conditionsIn);
        this.item = item;
        this.maxRoll = maxRoll;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(new ItemStack(item, context.getRandom().nextInt(maxRoll + 1)));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
