package mcp.mobius.waila.addons.capability;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.overlay.RayTracing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.List;

public class HUDHandlerInventory implements IWailaDataProvider {

    static final IWailaDataProvider INSTANCE = new HUDHandlerInventory();

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        if (!config.getConfig("capability.inventoryinfo") || accessor.getTileEntity() == null || accessor.getTileEntity().getClass() == TileEntityFurnace.class)
            return currenttip;

        if (accessor.getNBTData().hasKey("handler")) {
            int handlerSize = accessor.getNBTData().getInteger("handlerSize");
            IItemHandler itemHandler = new ItemStackHandler(handlerSize);
            populateInv((ItemStackHandler) itemHandler, accessor.getNBTData().getTagList("handler", 10));

            List<ItemStack> toRender = new ArrayList<ItemStack>();
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack == null)
                    continue;

                addStack(toRender, stack);
            }

            String renderString = "";
            int drawnCount = 0;
            for (ItemStack stack : toRender) {
                String name = stack.getItem().getRegistryName().toString();
                if (drawnCount > 5 && !accessor.getPlayer().isSneaking())
                    break;
                else if (drawnCount > 5 && accessor.getPlayer().isSneaking()) {
                    currenttip.add(renderString);
                    renderString = "";
                    drawnCount = 0;
                }

                renderString += SpecialChars.getRenderString("waila.stack", "1", name, String.valueOf(stack.stackSize), String.valueOf(stack.getItemDamage()));
                drawnCount += 1;
            }

            currenttip.add(renderString);
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, BlockPos pos) {
        if (te != null) {
            te.writeToNBT(tag);
            RayTraceResult rayTrace = RayTracing.rayTraceServer(player, player.capabilities.isCreativeMode ? 5.0 : 4.5);
            EnumFacing side = null;
            if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK)
                side = rayTrace.sideHit;

            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
                IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
                tag.setTag("handler", invToNBT(itemHandler));
                tag.setInteger("handlerSize", itemHandler.getSlots());
            } else if (te instanceof IInventory) {
                IItemHandler itemHandler = new InvWrapper((IInventory) te);
                tag.setTag("handler", invToNBT(itemHandler));
                tag.setInteger("handlerSize", itemHandler.getSlots());
            } else if (te instanceof TileEntityEnderChest) {
                IItemHandler itemHandler = new InvWrapper(player.getInventoryEnderChest());
                tag.setTag("handler", invToNBT(itemHandler));
                tag.setInteger("handlerSize", itemHandler.getSlots());
            }
        }

        return tag;
    }

    private NBTTagList invToNBT(IItemHandler itemHandler) {
        NBTTagList nbtTagList = new NBTTagList();
        int size = itemHandler.getSlots();
        for (int i = 0; i < size; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                writeStack(stack, itemTag);
                nbtTagList.appendTag(itemTag);
            }
        }

        return nbtTagList;
    }

    private void populateInv(IItemHandlerModifiable itemHandler, NBTTagList tagList) {
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
            int slot = itemTags.getInteger("Slot");

            if (slot >= 0 && slot < itemHandler.getSlots())
                itemHandler.setStackInSlot(slot, readStack(itemTags));
        }
    }

    private void writeStack(ItemStack stack, NBTTagCompound tagCompound) {
        stack.writeToNBT(tagCompound);
        tagCompound.setInteger("CountI", stack.stackSize);
    }

    private ItemStack readStack(NBTTagCompound tagCompound) {
        ItemStack stack = ItemStack.loadItemStackFromNBT(tagCompound);
        stack.stackSize = tagCompound.getInteger("CountI");
        return stack;
    }

    private void addStack(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack invStack : stacks) {
            if (ItemHandlerHelper.canItemStacksStack(invStack, stack)) {
                invStack.stackSize += stack.stackSize;
                return;
            }
        }

        stacks.add(stack.copy());
    }
}