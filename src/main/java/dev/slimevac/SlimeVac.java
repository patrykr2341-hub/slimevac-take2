package dev.slimevac;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class SlimeVac implements ModInitializer {
    public static final String MOD_ID = "slimevac";
    public static final Item SLIME_STEEL_BLEND = registerItem("slime_steel_blend", new Item(new Item.Settings()));
    public static final Item SLIME_STEEL_INGOT = registerItem("slime_steel_ingot", new Item(new Item.Settings()));
    public static final Item SLIME_CORE = registerItem("slime_core", new Item(new Item.Settings()));
    public static final Block VACPACK_MACHINE = Registry.register(Registries.BLOCK,
            id("vacpack_machine"), new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
    public static final Item VACPACK_MACHINE_ITEM = registerItem("vacpack_machine",
            new BlockItem(VACPACK_MACHINE, new Item.Settings()));
    public static final Item SLIMEVAC = registerItem("slimevac", new VacpackItem(new Item.Settings().maxCount(1)));

    private static Identifier id(String path) { return Identifier.of(MOD_ID, path); }
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SLIME_STEEL_BLEND);
            entries.add(SLIME_STEEL_INGOT);
            entries.add(SLIME_CORE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(VACPACK_MACHINE_ITEM);
            entries.add(SLIMEVAC);
        });

        // Right-clicking a mob with the Vacpack captures it immediately.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof VacpackItem) || !(entity instanceof MobEntity mob)
                    || player.isSneaking() || world.isClient) return ActionResult.PASS;
            if (hasCaptured(stack)) {
                player.sendMessage(Text.translatable("message.slimevac.full"), true);
                return ActionResult.FAIL;
            }
            if (mob.isRemoved() || !mob.isAlive()) return ActionResult.PASS;
            storeCaptured(stack, Registries.ENTITY_TYPE.getId(mob.getType()).toString());
            mob.discard();
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SLIME_BLOCK_FALL,
                    SoundCategory.PLAYERS, 0.8f, 1.2f);
            player.emitGameEvent(GameEvent.ENTITY_INTERACT);
            player.sendMessage(Text.translatable("message.slimevac.captured", mob.getName()), true);
            return ActionResult.SUCCESS;
        });
    }

    public static boolean hasCaptured(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().contains("CapturedMob");
    }

    public static String capturedId(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? "" : data.copyNbt().getString("CapturedMob");
    }

    public static void storeCaptured(ItemStack stack, String entityId) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putString("CapturedMob", entityId);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void clearCaptured(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.remove("CapturedMob");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static class VacpackItem extends Item {
        public VacpackItem(Settings settings) { super(settings); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (user.isSneaking()) {
                boolean release = !stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                        .copyNbt().getBoolean("ReleaseMode");
                NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                nbt.putBoolean("ReleaseMode", release);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                if (!world.isClient) user.sendMessage(Text.translatable(
                        release ? "message.slimevac.release_mode" : "message.slimevac.suction_mode"), true);
                return TypedActionResult.success(stack, world.isClient);
            }

            if (hasCaptured(stack)) {
                if (!world.isClient && world instanceof ServerWorld serverWorld) {
                    String id = capturedId(stack);
                    EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.tryParse(id));
                    if (type != null) {
                        Vec3d look = user.getRotationVec(1.0f);
                        Vec3d pos = user.getEyePos().add(look.multiply(1.8));
                        Entity spawned = type.spawn(serverWorld, user.getBlockPos().add(
                                (int)Math.round(look.x * 2), 0, (int)Math.round(look.z * 2)),
                                SpawnReason.TRIGGERED);
                        if (spawned != null) {
                            spawned.refreshPositionAndAngles(pos.x, pos.y, pos.z, user.getYaw(), user.getPitch());
                            clearCaptured(stack);
                            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH,
                                    SoundCategory.PLAYERS, 0.9f, 0.8f);
                        }
                    }
                }
                return TypedActionResult.success(stack, world.isClient);
            }
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        @Override public int getMaxUseTime(ItemStack stack) { return 72000; }

        @Override
        public void usageTick(World world, net.minecraft.entity.LivingEntity user, ItemStack stack, int remainingUseTicks) {
            if (world.isClient || !(user instanceof PlayerEntity player) || !(world instanceof ServerWorld)) return;
            Box area = player.getBoundingBox().expand(7.0, 5.0, 7.0);
            MobEntity target = world.getEntitiesByClass(MobEntity.class, area,
                    mob -> mob.isAlive() && !mob.isRemoved() && mob != player)
                    .stream()
                    .filter(mob -> !mob.isTeammate(player))
                    .filter(mob -> {
                        Vec3d to = mob.getEntityPos().subtract(player.getEyePos()).normalize();
                        return player.getRotationVec(1.0f).dotProduct(to) > 0.35;
                    })
                    .min((a, b) -> Double.compare(a.squaredDistanceTo(player), b.squaredDistanceTo(player)))
                    .orElse(null);
            if (target == null) return;
            Vec3d pull = player.getPos().subtract(target.getPos()).normalize().multiply(0.22);
            target.addVelocity(pull.x, pull.y + 0.025, pull.z);
            target.velocityModified = true;
            if (target.squaredDistanceTo(player) < 2.2 && !hasCaptured(stack)) {
                storeCaptured(stack, Registries.ENTITY_TYPE.getId(target.getType()).toString());
                target.discard();
                player.sendMessage(Text.translatable("message.slimevac.captured", target.getName()), true);
                player.stopUsingItem();
            }
        }

        @Override
        public void onStoppedUsing(ItemStack stack, World world, net.minecraft.entity.LivingEntity user, int remainingUseTicks) {
            super.onStoppedUsing(stack, world, user, remainingUseTicks);
        }
    }
}
