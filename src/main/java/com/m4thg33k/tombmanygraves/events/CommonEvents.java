package com.m4thg33k.tombmanygraves.events;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.blocks.ModBlocks;
import com.m4thg33k.tombmanygraves.invman.DeathInventoryHandler;
import com.m4thg33k.tombmanygraves.invman.InventoryHolder;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.tiles.TileGrave;
import com.m4thg33k.tombmanygraves.util.ChatHelper;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CommonEvents {

	private HashMap<UUID, InventoryHolder> invs = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerDeath(LivingDeathEvent e) {
		if (!ModConfigs.ENABLE_GRAVES) return;
		if (e.getEntityLiving().getEntityWorld().isRemote || !(e.getEntityLiving() instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) e.getEntityLiving();
		if (player.world.getGameRules().getBoolean("keepInventory")) return;
		InventoryHolder inventoryHolder = new InventoryHolder();
		inventoryHolder.grabPlayerData(player);
		invs.put(player.getUniqueID(), inventoryHolder);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerDrop(PlayerDropsEvent e) {
		if (!ModConfigs.ENABLE_GRAVES) return;
		EntityPlayer player = e.getEntityPlayer();
		if (ModConfigs.PRINT_DEATH_LOG) {
			handleDeathLog(player);
		}
		InventoryHolder inventoryHolder = invs.remove(player.getUniqueID());
		if (inventoryHolder == null) return;

		List<EntityItem> graveDrops = inventoryHolder.ensure(e.getDrops());
		if (inventoryHolder.isInventoryEmpty()) {
			if (ModConfigs.ENABLE_CHAT_MESSAGE_ON_DEATH) {
				ChatHelper.sayMessage(player, "But there were no valid items for the grave!");
			}
			return;
		}

		if (placeGrave(player, inventoryHolder)) {
			e.getDrops().removeAll(graveDrops);
		}

		if (ModConfigs.ALLOW_INVENTORY_SAVES) {
			if (!DeathInventoryHandler.writeInventoryFile(inventoryHolder)) {
				LogHelper.error("An error occurred when writing the inventory file for " + inventoryHolder.getPlayerName() + ".");
			}
			if (ModConfigs.ALLOW_INVENTORY_LISTS) {
				giveDeathList(player, inventoryHolder);
			}
		}

		for (EntityItem entityItem : e.getDrops()) {
			if (entityItem.getItem().getItem() == Item.getItemFromBlock(ModBlocks.blockGrave) || entityItem.getItem().getItem() == ModItems.itemDeathList) {
				entityItem.setDead();
			}
		}
	}

	private void giveDeathList(EntityPlayer player, InventoryHolder inventoryHolder) {
		ItemStack deathList = new ItemStack(ModItems.itemDeathList, 1);
		deathList.setTagCompound(inventoryHolder.writeToNBT(new NBTTagCompound()));
		InventoryPlayer inv = player.inventory;
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			if (inv.mainInventory.get(i).isEmpty()) {
				inv.mainInventory.set(i, deathList);
				break;
			}
		}
	}

	private void handleDeathLog(EntityPlayer player) {
		BlockPos pos = player.getPosition();
		LogHelper.info(player.getName() + " died in dimension " + player.dimension + " at " + pos.toString() + ". Their grave may be near!");
	}

	/** @return whether the grave was placed and now holds the items */
	private boolean placeGrave(EntityPlayer player, InventoryHolder inventoryHolder) {
		if (ModConfigs.ENABLE_CHAT_MESSAGE_ON_DEATH) {
			ChatHelper.sayMessage(player, "Place of death: " + player.getPosition().toString());
		}

		BlockPos posToPlace = new BlockPos(0, -1, 0);
		if (ModConfigs.ASCEND_LIQUID) {
			posToPlace = findValidGraveLocation(player.world, ascendFromFluid(player.world, player.getPosition()));
		}
		if (posToPlace.getY() == -1) {
			posToPlace = findValidGraveLocation(player.world, player.getPosition());
		}

		if (posToPlace.getY() == -1) {
			ChatHelper.sayMessage(player, "A suitable location for the grave wasn't found.");
			return false;
		}

		player.world.setBlockState(posToPlace, ModBlocks.blockGrave.getDefaultState());
		TileEntity tile = player.world.getTileEntity(posToPlace);
		if (!(tile instanceof TileGrave)) {
			LogHelper.info("Error! Grave could not be found after placement! " + posToPlace.toString());
			ChatHelper.sayMessage(player, "ERROR! Grave formed incorrectly! Report to M4thG33k!");
			player.world.setBlockToAir(posToPlace);
			return false;
		}

		TileGrave grave = (TileGrave) tile;
		grave.getPlayerData(player);

		IBlockState camoBlock = getBlockStateBelow(player.world, posToPlace);
		if (camoBlock.getMaterial() == Material.AIR) {
			camoBlock = ModBlocks.blockGrave.getDefaultState();
		} else if (camoBlock.getMaterial() == Material.GRASS) {
			camoBlock = Blocks.DIRT.getDefaultState();
		}

		grave.setCamoState(camoBlock);
		inventoryHolder.setPosition(posToPlace);
		grave.setSavedInventory(inventoryHolder);
		return true;
	}

	private BlockPos findValidGraveLocation(World world, BlockPos pos) {
		BlockPos toReturn = new BlockPos(-1, -1, -1);
		BlockPos toCheck = pos.add(0, 0, 0);

		if (toCheck.getY() <= 0) {
			toCheck = toCheck.add(0, MathHelper.abs(toCheck.getY()) + (ModConfigs.START_VOID_SEARCH_AT_ONE ? 1 : ModConfigs.MAX_GRAVE_SEARCH_RADIUS), 0);
		}

		for (int r = 0; r <= ModConfigs.MAX_GRAVE_SEARCH_RADIUS; r++) {
			toReturn = checkLevel(world, toCheck, r, false);
			if (toReturn.getY() != -1) {
				return toReturn;
			}
		}
		return toReturn;
	}

	private BlockPos ascendFromFluid(World world, BlockPos pos) {
		BlockPos toCheck = pos.add(0, 0, 0);
		int height = 0;
		while (pos.getY() + height < world.getActualHeight() - ModConfigs.MAX_GRAVE_SEARCH_RADIUS && !isValidLocation(world, toCheck, true)) {
			toCheck = checkLevel(world, pos.add(0, height, 0), 1, true);
			height += 1;
		}

		return toCheck;
	}

	private BlockPos checkLevel(World world, BlockPos pos, int radius, boolean ignoreFluidConfigs) {
		if (radius == 0 && isValidLocation(world, pos, ignoreFluidConfigs)) {
			return pos;
		}

		for (int i = -radius; i <= radius; i++) {
			for (int j = -radius; j <= radius; j++) {
				for (int k = radius; k >= -radius; k--) {
					if (MathHelper.abs(i) == radius || MathHelper.abs(j) == radius || MathHelper.abs(k) == radius) {
						if (isValidLocation(world, pos.add(i, j, k), ignoreFluidConfigs)) {
							return pos.add(i, j, k);
						}
					}
				}
			}
		}

		return new BlockPos(-1, -1, -1);
	}

	private boolean isValidLocation(World world, BlockPos pos, boolean ignoreFluidConfigs) {
		if (pos.getY() < 0 || pos.getY() >= world.getActualHeight()) {
			return false;
		}
		if (world.isAirBlock(pos)) {
			return true;
		}

		Block theBlock = world.getBlockState(pos).getBlock();
		if (ModConfigs.REPLACE_PLANTS && theBlock instanceof IPlantable) {
			return true;
		}

		if (!ignoreFluidConfigs) {
			if ((ModConfigs.REPLACE_STILL_LAVA && theBlock == Blocks.LAVA) || (ModConfigs.REPLACE_FLOWING_LAVA && theBlock == Blocks.FLOWING_LAVA) || (ModConfigs.REPLACE_STILL_WATER && theBlock == Blocks.WATER) || (ModConfigs.REPLACE_FLOWING_WATER && theBlock == Blocks.FLOWING_WATER)) {
				return true;
			}
		}

		return false;
	}

	private IBlockState getBlockStateBelow(World world, BlockPos pos) {
		return world.getBlockState(pos.add(0, -1, 0));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void itemToss(ItemTossEvent event) {
		Item item = event.getEntityItem().getItem().getItem();
		if (item == Item.getItemFromBlock(ModBlocks.blockGrave) || item == ModItems.itemDeathList) {
			event.setCanceled(true);
		}
	}

	// @SubscribeEvent(priority = EventPriority.HIGH)
	// public void itemDrop(PlayerDropsEvent event) {
	// if (event.getEntityPlayer() == null || event.getEntityPlayer() instanceof
	// FakePlayer || event.isCanceled()) {
	// return;
	// }
	//
	// if
	// (event.getEntityPlayer().world.getGameRules().getBoolean("keepInventory"))
	// {
	// return;
	// }
	//
	// event.getDrops().removeIf(ei -> ei.getItem().getItem() ==
	// ModItems.itemDeathList);
	//
	// // Get the death list from the current death
	// ItemStack newList =
	// DeathInventoryHandler.getDeathListFromFile(event.getEntityPlayer().getName(),
	// "latest");
	// if (newList != null) {
	// InventoryPlayer inv = event.getEntityPlayer().inventory;
	// for (int i = 0; i < inv.mainInventory.size(); i++) {
	// if (inv.mainInventory.get(i).isEmpty()) {
	// inv.mainInventory.set(i, newList.copy());
	// break;
	// }
	// }
	// }
	//
	// // for (EntityItem entityItem : event.getDrops())
	// // {
	// // if (entityItem.getItem().getItem() ==
	// // Item.getItemFromBlock(ModBlocks.blockGrave) ||
	// // entityItem.getItem().getItem() == ModItems.itemDeathList)
	// // {
	// // entityItem.setDead();
	// // }
	// // }
	// }

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerClone(PlayerEvent.Clone event) {
		if (ModConfigs.ENABLE_GRAVES && ModConfigs.ALLOW_INVENTORY_LISTS && !event.isCanceled() && !event.getEntityLiving().world.getGameRules().getBoolean("keepInventory") && ModConfigs.ALLOW_INVENTORY_SAVES && !(event.getEntityLiving().world.isRemote) && event.isWasDeath()) {

			// Get the death list from the current death
			for (int i = 0; i < event.getOriginal().inventory.mainInventory.size(); i++) {
				ItemStack item = event.getOriginal().inventory.mainInventory.get(i);

				if (!item.isEmpty() && item.getItem() == ModItems.itemDeathList) {
					for (int j = 0; j < event.getEntityPlayer().inventory.mainInventory.size(); j++) {
						if (event.getEntityPlayer().inventory.mainInventory.get(j).isEmpty()) {
							event.getEntityPlayer().inventory.mainInventory.set(j, item.copy());
							break;
						}
					}
				}
			}
		}
	}

}
