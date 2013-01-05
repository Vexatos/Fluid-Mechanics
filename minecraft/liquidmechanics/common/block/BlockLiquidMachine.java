package liquidmechanics.common.block;

import universalelectricity.prefab.BlockMachine;
import universalelectricity.prefab.tile.TileEntityAdvanced;
import liquidmechanics.client.render.BlockRenderHelper;
import liquidmechanics.common.LiquidMechanics;
import liquidmechanics.common.MetaGroup;
import liquidmechanics.common.TabLiquidMechanics;
import liquidmechanics.common.handlers.LiquidHandler;
import liquidmechanics.common.tileentity.TileEntityPump;
import liquidmechanics.common.tileentity.TileEntityTank;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;

public class BlockLiquidMachine extends BlockMachine
{

    public BlockLiquidMachine(int id)
    {
        super("lmMachines", id, Material.iron, TabLiquidMechanics.INSTANCE);
        this.setHardness(1f);
        this.setResistance(5f);
    }
    public void onBlockAdded(World world, int x, int y, int z) 
    {
        int meta = world.getBlockMetadata(x, y, z);
        if(MetaGroup.getGrouping(meta) == 1)
        {
            world.setBlockAndMetadata(x, y, z, LiquidMechanics.blockTank.blockID, 15);
        }
    }
    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return BlockRenderHelper.renderID;
    }

    @Override
    public int damageDropped(int meta)
    {
        if (meta < 4) { return 0; }
        return meta;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
    {
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity ent = world.getBlockTileEntity(x, y, z);

        if (meta < 4)
        {
            new ItemStack(LiquidMechanics.blockMachine, 1, 0);
        }
       
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving par5EntityLiving)
    {
        int meta = world.getBlockMetadata(x, y, z);
        int angle = MathHelper.floor_double((par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        TileEntity ent = world.getBlockTileEntity(x, y, z);

        world.setBlockMetadata(x, y, z, angle + MetaGroup.getGroupStartMeta(MetaGroup.getGrouping(meta)));
        if (ent instanceof TileEntityAdvanced)
        {
            ((TileEntityAdvanced) world.getBlockTileEntity(x, y, z)).initiate();
        }

        world.notifyBlocksOfNeighborChange(x, y, z, this.blockID);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int meta)
    {
        if (meta >= 12)
        {
        }
        else if (meta >= 8)
        {

        }
        else if (meta >= 4)
        {

        }
        else
        {
            return new TileEntityPump();
        }
        return null;
    }
}