package gd.rf.acro.platos.util;

import gd.rf.acro.platos.ConfigUtils;
import gd.rf.acro.platos.PlatosTransporters;
import gd.rf.acro.platos.entity.BlockShipEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Clearable;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ShipCreation {
    public static ActionResult create_ship(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand) {
        int balloons = Integer.parseInt(ConfigUtils.config.get("balloon"));
        int floats = Integer.parseInt(ConfigUtils.config.get("float"));
        int wheels = Integer.parseInt(ConfigUtils.config.get("wheel"));
        String whitelist = ConfigUtils.config.getOrDefault("whitelist", "false");
        int type = -1;
        int blocks = 0;
        int balances = 0;
        HashMap<String, Integer> used = new HashMap<>();
        ListTag list = new ListTag();
        ListTag addons = new ListTag();
        CompoundTag storage = new CompoundTag();
        Set<BlockPos> to_visit = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        int max_posx = 3;
        int max_posz = 3;
        int min_posx = -3;
        int min_posz = -3;
        int max_posy = 3;
        int min_posy = -3;
        to_visit.add(pos);
        visited.add(pos);
        while (!to_visit.isEmpty()) {
            BlockPos look_around_pos = to_visit.iterator().next();
            to_visit.remove(look_around_pos);
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    for (int z = -1; z < 2; z++) {
                        BlockPos current_pos = look_around_pos.add(x, y, z);

                        if (!world.getBlockState(current_pos).isAir() &&
                                !world.getBlockState(current_pos).getBlock().getTranslationKey().contains("ore")
                                && world.getBlockState(current_pos).getFluidState().isEmpty()
                                &&
                                ((whitelist.equals("true") && PlatosTransporters.BOAT_MATERIAL.contains(world.getBlockState(current_pos).getBlock()))
                                        || (whitelist.equals("false") && !PlatosTransporters.BOAT_MATERIAL_BLACKLIST.contains(world.getBlockState(current_pos).getBlock())))) {
                            if (!visited.add(current_pos)) {
                                //skip center
                                continue;
                            }

                            if (current_pos.getX() > max_posx) {
                                max_posx = current_pos.getX();
                            }
                            if (current_pos.getX() < min_posx) {
                                min_posx = current_pos.getX();
                            }
                            if (current_pos.getY() > max_posy) {
                                max_posy = current_pos.getY();
                            }
                            if (current_pos.getY() < min_posy) {
                                min_posy = current_pos.getY();
                            }
                            if (current_pos.getZ() > max_posz) {
                                max_posz = current_pos.getZ();
                            }
                            if (current_pos.getZ() < min_posz) {
                                min_posz = current_pos.getZ();
                            }
                            to_visit.add(current_pos);
                        }
                    }
                }
            }
        }

        for (Iterator<BlockPos> blockPosIterator = visited.iterator(); blockPosIterator.hasNext(); ) {
            BlockPos corrent_pos = blockPosIterator.next();
            BlockPos diff = corrent_pos.subtract(pos);
            int i = diff.getX();
            int j = diff.getX();
            int k = diff.getX();

            addIfCan(used, world.getBlockState(corrent_pos).getBlock().getTranslationKey(), 1);


            BlockState blockState = world.getBlockState(corrent_pos);
            list.add(StringTag.of(
                    Block.getRawIdFromState(blockState) + " " + i + " " + j + " " + k));
            blocks++;

            if (world.getBlockState(corrent_pos).getBlock() == Blocks.BLAST_FURNACE) {
                addons.add(StringTag.of("engine"));
            }
            if (world.getBlockState(corrent_pos).getBlock() == Blocks.REDSTONE_LAMP) {
                addons.add(StringTag.of("altitude"));
            }

            if (world.getBlockEntity(corrent_pos) != null) {
                CompoundTag data = world.getBlockEntity(corrent_pos).toTag(new CompoundTag());
                storage.put(i + " " + j + " " + k, data);
            }


            if (world.getBlockState(corrent_pos).getBlock() == PlatosTransporters.FLOAT_BLOCK && (type == 0 || type == -1)) {
                type = 0; //watership
                balances += floats;

            }
            if (world.getBlockState(corrent_pos).getBlock() == PlatosTransporters.BALLOON_BLOCK && (type == 1 || type == -1)) {
                type = 1; //airship
                balances += balloons;

            }
            if (world.getBlockState(corrent_pos).getBlock() == PlatosTransporters.WHEEL_BLOCK && (type == 2 || type == -1)) {
                type = 2; //carriage
                balances += wheels;
            }
        }
        System.out.println("blocks: " + blocks);
        System.out.println("balances: " + balances);
        if (type == -1) {
            player.sendMessage(new LiteralText("No wheel/float/balloon found"), false);
            return ActionResult.FAIL;
        }
        if (balances < blocks) {
            player.sendMessage(new LiteralText("Cannot assemble, not enough floats/balloons/wheels"), false);
            if (type == 0) {
                player.sendMessage(new LiteralText("Requires " + blocks / floats + " floats, you have " + balances / floats), false);
            }
            if (type == 1) {
                player.sendMessage(new LiteralText("Requires " + blocks / balloons + " balloons, you have " + balances / balloons), false);
            }
            if (type == 2) {
                player.sendMessage(new LiteralText("Requires " + blocks / wheels + " wheels, you have" + balances / wheels), false);
            }
            return ActionResult.FAIL;
        }
        list.forEach(block ->
        {
            String[] vv = block.asString().split(" ");
            if (world.getBlockEntity(pos.add(Integer.parseInt(vv[1]), Integer.parseInt(vv[2]), Integer.parseInt(vv[3]))) != null) {
                Clearable.clear(world.getBlockEntity(pos.add(Integer.parseInt(vv[1]), Integer.parseInt(vv[2]), Integer.parseInt(vv[3]))));
            }
            world.setBlockState(pos.add(Integer.parseInt(vv[1]), Integer.parseInt(vv[2]), Integer.parseInt(vv[3])), Blocks.AIR.getDefaultState());
        });

        BlockShipEntity entity = new BlockShipEntity(PlatosTransporters.BLOCK_SHIP_ENTITY_ENTITY_TYPE, world);
        int offset = 1;
        if (player.getStackInHand(hand).getItem() == PlatosTransporters.LIFT_JACK_ITEM) {
            if (player.getStackInHand(hand).hasTag()) {
                offset = player.getStackInHand(hand).getTag().getInt("off");
            }
        }
        entity.setBoundingBox(new Box(min_posx, min_posy, min_posz, max_posx, min_posy, min_posz));
        entity.setModel(list, getDirection(state), offset, type, storage, addons);
        entity.teleport(player.getX(), player.getY(), player.getZ());
        world.spawnEntity(entity);
        player.startRiding(entity, true);
        return ActionResult.SUCCESS;
    }

    private static int getDirection(BlockState state) {
        if (state.get(Properties.HORIZONTAL_FACING) == Direction.EAST) {
            return 90;
        }
        if (state.get(Properties.HORIZONTAL_FACING) == Direction.SOUTH) {
            return 180;
        }
        if (state.get(Properties.HORIZONTAL_FACING) == Direction.WEST) {
            return 270;
        }
        return 0;
    }

    private static HashMap<String, Integer> addIfCan(HashMap<String, Integer> input, String key, int mod) {
        if (input.containsKey(key)) {
            input.put(key, input.get(key) + mod);
        } else {
            input.put(key, mod);
        }
        return input;
    }
}
