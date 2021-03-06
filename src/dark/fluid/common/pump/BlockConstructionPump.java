package dark.fluid.common.pump;

import java.util.List;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

import com.builtbroken.common.Pair;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.core.prefab.tilenetwork.fluid.FluidNetworkHelper;
import dark.fluid.client.render.BlockRenderHelper;
import dark.fluid.common.FMRecipeLoader;
import dark.fluid.common.FluidMech;
import dark.fluid.common.machines.BlockFM;

public class BlockConstructionPump extends BlockFM
{
    Icon inputIcon;
    Icon outputIcon;

    public BlockConstructionPump()
    {
        super(BlockConstructionPump.class, "ConstructionPump", Material.iron);
        this.setHardness(1f);
        this.setResistance(5f);

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister par1IconRegister)
    {
        this.blockIcon = par1IconRegister.registerIcon(FluidMech.instance.PREFIX + "ironMachineSide");
        this.inputIcon = par1IconRegister.registerIcon(FluidMech.instance.PREFIX + "inputMachineSide");
        this.outputIcon = par1IconRegister.registerIcon(FluidMech.instance.PREFIX + "outputMachineSide");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon getBlockTexture(IBlockAccess world, int x, int y, int z, int side)
    {
        TileEntity entity = world.getBlockTileEntity(x, y, z);
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (entity instanceof TileEntityConstructionPump)
        {

            if (dir == ((TileEntityConstructionPump) entity).getFacing(false))
            {
                return this.outputIcon;
            }
            if (dir == ((TileEntityConstructionPump) entity).getFacing(true))
            {
                return this.inputIcon;
            }
        }
        return this.blockIcon;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public int damageDropped(int meta)
    {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType()
    {
        return BlockRenderHelper.renderID;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
    {
        return new ItemStack(FMRecipeLoader.blockConPump, 1, 0);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase p, ItemStack itemStack)
    {
    }

    @Override
    public TileEntity createNewTileEntity(World var1)
    {
        return new TileEntityConstructionPump();
    }

    @Override
    public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(new ItemStack(par1, 1, 0));
    }

    @Override
    public boolean onSneakUseWrench(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote)
        {
            int meta = world.getBlockMetadata(x, y, z);
            int angle = MathHelper.floor_double((entityPlayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;

            TileEntity entity = world.getBlockTileEntity(x, y, z);
            if (entity instanceof TileEntityConstructionPump)
            {
                FluidNetworkHelper.invalidate(entity);
            }

            if (meta == 3)
            {
                world.setBlockMetadataWithNotify(x, y, z, 0, 3);
            }
            else
            {
                world.setBlockMetadataWithNotify(x, y, z, meta + 1, 3);
            }

            return true;
        }
        return this.onUseWrench(world, x, y, z, entityPlayer, side, hitX, hitY, hitZ);
    }

    @Override
    public void getTileEntities(int blockID, Set<Pair<String, Class<? extends TileEntity>>> list)
    {
        list.add(new Pair<String, Class<? extends TileEntity>>("ConstructionPump", TileEntityConstructionPump.class));

    }

}
