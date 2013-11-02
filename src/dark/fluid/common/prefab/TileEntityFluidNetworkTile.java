package dark.fluid.common.prefab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;

import org.bouncycastle.util.Arrays;

import universalelectricity.core.vector.Vector3;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.api.fluid.FluidMasterList;
import dark.api.fluid.INetworkFluidPart;
import dark.api.parts.INetworkPart;
import dark.core.common.DarkMain;
import dark.core.network.ISimplePacketReceiver;
import dark.core.network.PacketHandler;
import dark.core.prefab.tilenetwork.NetworkTileEntities;
import dark.core.prefab.tilenetwork.fluid.NetworkFluidTiles;
import dark.fluid.common.FluidPartsMaterial;

public abstract class TileEntityFluidNetworkTile extends TileEntityFluidDevice implements INetworkFluidPart, ISimplePacketReceiver
{
    private int updateTick = 1;
    protected FluidTank tank;
    protected FluidTankInfo[] internalTanksInfo = new FluidTankInfo[1];
    protected List<TileEntity> connectedBlocks = new ArrayList<TileEntity>();
    public boolean[] renderConnection = new boolean[6];
    public boolean[] canConnectSide = new boolean[] { true, true, true, true, true, true, true };
    protected int heat = 0, maxHeat = 20000;
    protected int damage = 0, maxDamage = 1000;
    protected int subID = 0;
    protected int tankCap;

    protected NetworkFluidTiles network;

    public TileEntityFluidNetworkTile()
    {
        this(1);
    }

    public TileEntityFluidNetworkTile(int tankCap)
    {
        if (tankCap <= 0)
        {
            tankCap = 1;
        }
        this.tankCap = tankCap;
        this.tank = new FluidTank(this.tankCap * FluidContainerRegistry.BUCKET_VOLUME);
        this.internalTanksInfo[0] = this.tank.getInfo();
    }

    public FluidTank getTank()
    {
        if (tank == null)
        {
            this.tank = new FluidTank(this.tankCap * FluidContainerRegistry.BUCKET_VOLUME);
            this.internalTanksInfo[0] = this.tank.getInfo();
        }
        return tank;
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        if (!worldObj.isRemote)
        {
            if (ticks % this.updateTick == 0)
            {
                this.updateTick = this.worldObj.rand.nextInt(5) * 40 + 20;
                this.refresh();
            }
        }
    }

    @Override
    public void invalidate()
    {
        this.getTileNetwork().splitNetwork(this.worldObj, this);
        super.invalidate();
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        if (this.getTileNetwork() != null && this.canConnectSide[from.ordinal()] && resource != null)
        {
            return this.getTileNetwork().fillNetworkTank(this.worldObj, resource, doFill);
        }
        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (this.getTileNetwork() != null && this.canConnectSide[from.ordinal()] && resource != null)
        {
            if (this.getTileNetwork().getNetworkTank() != null && this.getTileNetwork().getNetworkTank().getFluid() != null && this.getTileNetwork().getNetworkTank().getFluid().isFluidEqual(resource))
            {
                this.getTileNetwork().drainNetworkTank(this.worldObj, resource.amount, doDrain);
            }

        }
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        if (this.getTileNetwork() != null && this.canConnectSide[from.ordinal()])
        {
            this.getTileNetwork().drainNetworkTank(this.worldObj, maxDrain, doDrain);
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return this.canConnectSide[from.ordinal()] && this.damage < this.maxDamage;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return this.canConnectSide[from.ordinal()] && this.damage < this.maxDamage;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        return new FluidTankInfo[] { this.getTileNetwork().getNetworkTankInfo() };
    }

    @Override
    public List<TileEntity> getNetworkConnections()
    {
        return this.connectedBlocks;
    }

    @Override
    public void refresh()
    {
        if (this.worldObj != null && !this.worldObj.isRemote)
        {
            boolean[] previousConnections = this.renderConnection.clone();
            this.connectedBlocks.clear();
            this.renderConnection = new boolean[6];

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
            {
                this.validateConnectionSide(new Vector3(this).modifyPositionFromSide(dir).getTileEntity(this.worldObj), dir);

            }
            /** Only send packet updates if visuallyConnected changed. */
            if (!Arrays.areEqual(previousConnections, this.renderConnection))
            {
                this.sendRenderUpdate();
            }
        }

    }

    /** Checks to make sure the connection is valid to the tileEntity
     *
     * @param tileEntity - the tileEntity being checked
     * @param side - side the connection is too */
    public void validateConnectionSide(TileEntity tileEntity, ForgeDirection side)
    {
        if (!this.worldObj.isRemote)
        {
            if (tileEntity instanceof INetworkFluidPart)
            {
                if (this.canTileConnect(Connection.NETWORK, side.getOpposite()))
                {
                    this.getTileNetwork().merge(((INetworkFluidPart) tileEntity).getTileNetwork(), (INetworkPart) tileEntity);
                    this.renderConnection[side.ordinal()] = true;
                    connectedBlocks.add(tileEntity);
                }
            }
        }
    }

    @Override
    public NetworkFluidTiles getTileNetwork()
    {
        if (!(this.network instanceof NetworkFluidTiles))
        {
            this.network = new NetworkFluidTiles(this);
        }
        return this.network;
    }

    @Override
    public void setTileNetwork(NetworkTileEntities fluidNetwork)
    {
        if (fluidNetwork instanceof NetworkFluidTiles)
        {
            this.network = (NetworkFluidTiles) fluidNetwork;
        }

    }

    @Override
    public boolean mergeDamage(String result)
    {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo()
    {
        if (this.internalTanksInfo == null)
        {
            this.internalTanksInfo = new FluidTankInfo[] { this.getTank().getInfo() };
        }
        return this.internalTanksInfo;
    }

    @Override
    public int fillTankContent(int index, FluidStack stack, boolean doFill, boolean update)
    {
        if (index == 0)
        {
            int p = this.getTank().getFluid() != null ? this.getTank().getFluid().amount : 0;
            int fill = this.getTank().fill(stack, doFill);
            if (p != fill)
            {
                //TODO add a catch to this so we don't send a dozen packets for one updates
                if (update)
                {
                    this.sendTankUpdate(index);
                }
                this.internalTanksInfo[index] = this.getTank().getInfo();
            }
            return fill;
        }
        return 0;
    }

    @Override
    public FluidStack drainTankContent(int index, int volume, boolean doDrain, boolean update)
    {
        if (index == 0)
        {
            FluidStack prev = this.getTank().getFluid();
            FluidStack stack = this.getTank().drain(volume, doDrain);
            if (prev != null && (stack == null || prev.amount != stack.amount))
            {
                if (update)
                {
                    this.sendTankUpdate(index);
                }
                this.internalTanksInfo[index] = this.getTank().getInfo();
            }
            return stack;
        }
        return null;
    }

    @Override
    public boolean canTileConnect(Connection type, ForgeDirection dir)
    {
        if (this.damage >= this.maxDamage)
        {
            return false;
        }
        return type == Connection.FLUIDS || type == Connection.NETWORK;
    }

    @Override
    public boolean canPassThrew(FluidStack fluid, ForgeDirection from, ForgeDirection to)
    {
        return this.connectedBlocks.get(from.ordinal()) != null && this.connectedBlocks.get(to.ordinal()) != null && this.damage < this.maxDamage;
    }

    @Override
    public boolean onPassThrew(FluidStack fluid, ForgeDirection from, ForgeDirection to)
    {
        FluidPartsMaterial mat = FluidPartsMaterial.get(this.getBlockMetadata());
        if (fluid != null && fluid.getFluid() != null && mat != null)
        {
            if (fluid.getFluid().isGaseous(fluid) && !mat.canSupportGas)
            {
                //TODO lose 25% of the gas, and render the lost
            }
            else if (FluidMasterList.isMolten(fluid.getFluid()) && !mat.canSupportMoltenFluids)
            {
                //TODO start to heat up the pipe to melting point. When it hits melting point turn the pipe to its molten metal equal
                //TODO also once it reaches a set heat level start burning up blocks around the pipe. Eg wood
                this.heat += FluidMasterList.getHeatPerPass(fluid.getFluid());
                if (heat >= this.maxHeat)
                {
                    this.worldObj.setBlock(xCoord, yCoord, zCoord, Block.lavaStill.blockID);
                    return true;
                }
            }
            else if (!fluid.getFluid().isGaseous(fluid) && !mat.canSupportFluids)
            {
                this.damage += 1;
                if (this.damage >= this.maxDamage)
                {
                    //TODO test this and make sure its right, as well black fluid block in some cases
                    this.getBlockType().dropBlockAsItem(worldObj, xCoord, yCoord, zCoord, 0, 0);
                    this.worldObj.setBlock(xCoord, yCoord, zCoord, 0);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.damage = nbt.getInteger("damage");
        this.heat = nbt.getInteger("heat");
        this.subID = nbt.getInteger("subID");
        if (nbt.hasKey("stored"))
        {
            NBTTagCompound tag = nbt.getCompoundTag("stored");
            String name = tag.getString("LiquidName");
            int amount = nbt.getInteger("Amount");
            Fluid fluid = FluidRegistry.getFluid(name);
            if (fluid != null)
            {
                FluidStack liquid = new FluidStack(fluid, amount);
                this.getTank().setFluid(liquid);
                internalTanksInfo[0] = this.getTank().getInfo();
            }
        }
        else
        {
            this.getTank().readFromNBT(nbt.getCompoundTag("FluidTank"));
            internalTanksInfo[0] = this.getTank().getInfo();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setInteger("damage", this.damage);
        nbt.setInteger("heat", this.heat);
        nbt.setInteger("subID", this.subID);
        nbt.setCompoundTag("FluidTank", this.getTank().writeToNBT(new NBTTagCompound()));
    }

    @Override
    public boolean simplePacket(String id, ByteArrayDataInput data, Player player)
    {
        try
        {
            if (this.worldObj.isRemote)
            {
                if (id.equalsIgnoreCase("DescriptionPacket"))
                {
                    this.subID = data.readInt();
                    this.renderConnection[0] = data.readBoolean();
                    this.renderConnection[1] = data.readBoolean();
                    this.renderConnection[2] = data.readBoolean();
                    this.renderConnection[3] = data.readBoolean();
                    this.renderConnection[4] = data.readBoolean();
                    this.renderConnection[5] = data.readBoolean();
                    this.tank = new FluidTank(data.readInt());
                    this.getTank().readFromNBT(PacketHandler.instance().readNBTTagCompound(data));
                    this.internalTanksInfo[0] = this.getTank().getInfo();
                    return true;
                }
                else if (id.equalsIgnoreCase("RenderPacket"))
                {
                    this.subID = data.readInt();
                    this.renderConnection[0] = data.readBoolean();
                    this.renderConnection[1] = data.readBoolean();
                    this.renderConnection[2] = data.readBoolean();
                    this.renderConnection[3] = data.readBoolean();
                    this.renderConnection[4] = data.readBoolean();
                    this.renderConnection[5] = data.readBoolean();
                    return true;
                }
                else if (id.equalsIgnoreCase("SingleTank"))
                {
                    this.tank = new FluidTank(data.readInt());
                    this.getTank().readFromNBT(PacketHandler.instance().readNBTTagCompound(data));
                    this.internalTanksInfo[0] = this.getTank().getInfo();
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("// Fluid Mechanics Tank packet read error");
            e.printStackTrace();
            return true;
        }
        return false;
    }

    @Override
    public Packet getDescriptionPacket()
    {
        Object[] data = new Object[10];
        data[0] = "DescriptionPacket";
        data[1] = this.subID;
        data[2] = this.renderConnection[0];
        data[3] = this.renderConnection[1];
        data[4] = this.renderConnection[2];
        data[5] = this.renderConnection[3];
        data[6] = this.renderConnection[4];
        data[7] = this.renderConnection[5];
        data[8] = this.getTank().getCapacity();
        data[9] = this.getTank().writeToNBT(new NBTTagCompound());
        return PacketHandler.instance().getPacket(DarkMain.CHANNEL, this, data);
    }

    public void sendRenderUpdate()
    {
        Object[] data = new Object[8];
        data[0] = "RenderPacket";
        data[1] = this.subID;
        data[2] = this.renderConnection[0];
        data[3] = this.renderConnection[1];
        data[4] = this.renderConnection[2];
        data[5] = this.renderConnection[3];
        data[6] = this.renderConnection[4];
        data[7] = this.renderConnection[5];
        PacketHandler.instance().sendPacketToClients(PacketHandler.instance().getPacket(DarkMain.CHANNEL, this, data));
    }

    public void sendTankUpdate(int index)
    {
        if (this.getTank() != null && index == 0)
        {
            PacketHandler.instance().sendPacketToClients(PacketHandler.instance().getPacket(DarkMain.CHANNEL, this, "SingleTank", this.getTank().getCapacity(), this.getTank().writeToNBT(new NBTTagCompound())));
        }
    }

    @Override
    public String getMeterReading(EntityPlayer user, ForgeDirection side, EnumTools tool)
    {
        if (tool == EnumTools.PIPE_GUAGE)
        {
            String out = "Debug: " + this.getTileNetwork().toString();
            out += "   ";
            for (boolean b : this.renderConnection)
            {
                out += "|" + (b ? "T" : "F");
            }
            return out + "   Vol: " + this.getTileNetwork().getNetworkTank().getFluidAmount();
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return AxisAlignedBB.getAABBPool().getAABB(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1);
    }

    public int getSubID()
    {
        return this.subID;
    }

    public void setSubID(int id)
    {
        this.subID = id;
    }

}