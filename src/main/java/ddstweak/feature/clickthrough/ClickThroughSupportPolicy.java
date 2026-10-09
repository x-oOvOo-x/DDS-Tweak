package ddstweak.feature.clickthrough;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
//#if MC >= 11500
import net.minecraft.world.level.block.BeehiveBlock;
//#endif
//#if MC >= 11600
import net.minecraft.world.level.block.RespawnAnchorBlock;
//#endif
//#if MC >= 11700
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
//#endif
//#if MC >= 12000
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
//#endif
//#if MC >= 12004
import net.minecraft.world.level.block.WeatheringCopperDoorBlock;
import net.minecraft.world.level.block.WeatheringCopperTrapDoorBlock;
//#endif
//#if MC >= 12111
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
//#endif

public final class ClickThroughSupportPolicy {
    private ClickThroughSupportPolicy(){}

    public static boolean canClickThrough(ClientLevel level,LocalPlayer player,BlockHitResult hit){
        BlockPos pos=hit.getBlockPos();
        BlockState state=level.getBlockState(pos);
        return state.getMenuProvider(level,pos)!=null||hasPredictableVanillaInteraction(level,player,hit,state);
    }

    private static boolean hasPredictableVanillaInteraction(ClientLevel level,LocalPlayer player,BlockHitResult hit,BlockState state){
        Block block=state.getBlock();
        Class<?> type=block.getClass();

        if(type==NoteBlock.class)return true;
        if(type==JukeboxBlock.class)return canUseJukebox(level,player,hit.getBlockPos(),state);
        if(type==ComposterBlock.class)return canUseComposter(player,state);
        if(type==DaylightDetectorBlock.class)return mayBuild(player);
        if(type==CampfireBlock.class)return canUseCampfire(level,player,state);
        if(type==BellBlock.class)return canRingBell(state,hit);
        if(type==TntBlock.class)return holds(player,Items.FLINT_AND_STEEL)||holds(player,Items.FIRE_CHARGE);
        if(type==PumpkinBlock.class)return holds(player,Items.SHEARS);
        if(type==DragonEggBlock.class)return true;
        if(type==CommandBlock.class||type==StructureBlock.class||type==JigsawBlock.class)
            return player.canUseGameMasterBlocks()&&level.getBlockEntity(hit.getBlockPos())!=null;
        if(type==RepeaterBlock.class||type==ComparatorBlock.class)return mayBuild(player);

        if(isHandOpenableDoor(block,type)||isHandOpenableTrapDoor(block,type)||type==FenceGateBlock.class||type==BedBlock.class)return true;
        if(type==EnderChestBlock.class)return canUseEnderChest(level,hit.getBlockPos());
        if(type==LecternBlock.class)return canUseLectern(player,state);
        if(type==CakeBlock.class)return canUseCake(player,state);

        //#if MC >= 11500
        if(type==BeehiveBlock.class)return canUseBeehive(player,state);
        //#endif
        //#if MC >= 11600
        if(type==RespawnAnchorBlock.class)return canUseRespawnAnchor(player,state);
        //#endif
        //#if MC >= 11700
        if(type==CandleCakeBlock.class)return canUseCandleCake(player,hit,state);
        //#endif
        //#if MC >= 12000
        if(type==ChiseledBookShelfBlock.class)
            return hit.getDirection()==state.getValue(HorizontalDirectionalBlock.FACING)&&level.getBlockEntity(hit.getBlockPos())!=null;
        if(type==DecoratedPotBlock.class)return level.getBlockEntity(hit.getBlockPos())!=null;
        //#endif
        //#if MC >= 12111
        if(type==ShelfBlock.class)return canUseShelf(level,player,hit,state);
        //#endif

        // Stateful/server-defined interactions such as cauldrons, vaults and lead binding are
        // intentionally left to ALWAYS mode instead of guessing their result client-side.
        return false;
    }

    private static boolean canUseJukebox(ClientLevel level,LocalPlayer player,BlockPos pos,BlockState state){
        boolean playable=ClickThroughCompat.isJukeboxPlayable(player.getMainHandItem())
                ||ClickThroughCompat.isJukeboxPlayable(player.getOffhandItem());
        return ClickThroughSupportRules.jukebox(
                level.getBlockEntity(pos)!=null,
                state.getValue(JukeboxBlock.HAS_RECORD),
                playable
        );
    }

    private static boolean canUseComposter(LocalPlayer player,BlockState state){
        boolean compostable=ClickThroughCompat.isCompostable(player.getMainHandItem())
                ||ClickThroughCompat.isCompostable(player.getOffhandItem());
        return ClickThroughSupportRules.composter(state.getValue(ComposterBlock.LEVEL),compostable);
    }

    private static boolean canUseCampfire(ClientLevel level,LocalPlayer player,BlockState state){
        boolean cookable=ClickThroughCompat.isCampfireInput(level,player.getMainHandItem())
                ||ClickThroughCompat.isCampfireInput(level,player.getOffhandItem());
        boolean dousing=state.getValue(BlockStateProperties.LIT)
                &&(ClickThroughCompat.isCampfireDousingItem(player.getMainHandItem())
                ||ClickThroughCompat.isCampfireDousingItem(player.getOffhandItem()));
        return ClickThroughSupportRules.campfire(cookable,dousing);
    }

    private static boolean canRingBell(BlockState state,BlockHitResult hit){
        Direction direction=hit.getDirection();
        double clickY=hit.getLocation().y-hit.getBlockPos().getY();
        if(direction.getAxis()==Direction.Axis.Y||clickY>0.8124D)return false;
        Direction facing=state.getValue(BellBlock.FACING);
        BellAttachType attachment=state.getValue(BlockStateProperties.BELL_ATTACHMENT);
        if(attachment==BellAttachType.FLOOR)return facing.getAxis()==direction.getAxis();
        if(attachment==BellAttachType.SINGLE_WALL||attachment==BellAttachType.DOUBLE_WALL)return facing.getAxis()!=direction.getAxis();
        return attachment==BellAttachType.CEILING;
    }

    private static boolean isHandOpenableDoor(Block block,Class<?> type){
        if(type==DoorBlock.class)return block!=Blocks.IRON_DOOR;
        //#if MC >= 12004
        if(type==WeatheringCopperDoorBlock.class)return true;
        //#endif
        return false;
    }

    private static boolean isHandOpenableTrapDoor(Block block,Class<?> type){
        if(type==TrapDoorBlock.class)return block!=Blocks.IRON_TRAPDOOR;
        //#if MC >= 12004
        if(type==WeatheringCopperTrapDoorBlock.class)return true;
        //#endif
        return false;
    }

    private static boolean canUseEnderChest(ClientLevel level,BlockPos pos){
        boolean hasEntity=level.getBlockEntity(pos) instanceof EnderChestBlockEntity;
        BlockPos above=pos.above();
        boolean blocked=level.getBlockState(above).isRedstoneConductor(level,above);
        return ClickThroughSupportRules.enderChest(hasEntity,blocked);
    }

    private static boolean canUseLectern(LocalPlayer player,BlockState state){
        boolean hasBook=state.getValue(LecternBlock.HAS_BOOK);
        boolean lecternBook=isLecternBook(player.getMainHandItem())||isLecternBook(player.getOffhandItem());
        return ClickThroughSupportRules.lectern(hasBook,lecternBook);
    }

    private static boolean isLecternBook(ItemStack stack){
        return stack.getItem()==Items.WRITABLE_BOOK||stack.getItem()==Items.WRITTEN_BOOK;
    }

    private static boolean canUseCake(LocalPlayer player,BlockState state){
        boolean candle=false;
        //#if MC >= 11700
        candle=state.getValue(CakeBlock.BITES)==0
                &&(isCandle(player.getMainHandItem())||isCandle(player.getOffhandItem()));
        //#endif
        return ClickThroughSupportRules.cake(player.canEat(false),candle);
    }

    //#if MC >= 11700
    private static boolean canUseCandleCake(LocalPlayer player,BlockHitResult hit,BlockState state){
        ItemStack main=player.getMainHandItem();
        ItemStack off=player.getOffhandItem();
        boolean lit=state.getValue(BlockStateProperties.LIT);
        boolean upper=hit.getLocation().y-hit.getBlockPos().getY()>0.5D;
        boolean mainIgniter=isIgniter(main);
        boolean canEat=player.canEat(false)&&!mainIgniter;
        boolean canExtinguish=lit&&upper&&(main.isEmpty()||mainIgniter&&off.isEmpty());
        boolean canIgnite=!lit&&(isIgniter(main)||isIgniter(off));
        return ClickThroughSupportRules.candleCake(canEat,canExtinguish,canIgnite);
    }

    private static boolean isCandle(ItemStack stack){
        return !stack.isEmpty()&&Block.byItem(stack.getItem()) instanceof CandleBlock;
    }
    //#endif

    //#if MC >= 11500
    private static boolean canUseBeehive(LocalPlayer player,BlockState state){
        return ClickThroughSupportRules.beehive(
                state.getValue(BeehiveBlock.HONEY_LEVEL),
                holds(player,Items.SHEARS)||holds(player,Items.GLASS_BOTTLE)
        );
    }
    //#endif

    //#if MC >= 11600
    private static boolean canUseRespawnAnchor(LocalPlayer player,BlockState state){
        return ClickThroughSupportRules.respawnAnchor(
                state.getValue(RespawnAnchorBlock.CHARGE),
                holds(player,Items.GLOWSTONE)
        );
    }
    //#endif

    //#if MC >= 12111
    private static boolean canUseShelf(ClientLevel level,LocalPlayer player,BlockHitResult hit,BlockState state){
        if(hit.getDirection()!=state.getValue(ShelfBlock.FACING))return false;
        BlockEntity blockEntity=level.getBlockEntity(hit.getBlockPos());
        if(!(blockEntity instanceof ShelfBlockEntity))return false;
        java.util.OptionalInt slot=((ShelfBlock)state.getBlock()).getHitSlot(hit,state.getValue(ShelfBlock.FACING));
        if(!slot.isPresent())return false;
        if(state.getValue(ShelfBlock.POWERED)||!player.getMainHandItem().isEmpty())return true;
        return !((ShelfBlockEntity)blockEntity).getItem(slot.getAsInt()).isEmpty();
    }
    //#endif

    private static boolean isIgniter(ItemStack stack){
        return stack.getItem()==Items.FLINT_AND_STEEL||stack.getItem()==Items.FIRE_CHARGE;
    }

    private static boolean mayBuild(LocalPlayer player){
        //#if MC >= 11700
        return player.getAbilities().mayBuild;
        //#else
        //$$ return player.abilities.mayBuild;
        //#endif
    }

    private static boolean holds(LocalPlayer player,net.minecraft.world.item.Item item){
        return player.getMainHandItem().getItem()==item||player.getOffhandItem().getItem()==item;
    }
}
