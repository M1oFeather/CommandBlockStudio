package com.miofeather.commandblockstudio.main;

import com.miofeather.commandblockstudio.main.util.Pair;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

import java.util.LinkedList;
import java.util.List;

public class ChainHandler {
    private final BlockState current;
    private BlockState next;
    private final BlockPos currentPos;
    private final List<Pair<BlockState, Direction>> prior;
    private final Level world;

    public ChainHandler(Level world, BlockPos pos){
        this.world = world;
        this.currentPos = pos;
        this.current = world.getBlockState(currentPos);
        prior = new LinkedList<>();
        scanChain();
    }

    public void scanChain(){
        Direction currentDir = current.getValue(CommandBlock.FACING);
        BlockState up = getCommandBlock(currentPos.offset(new Vec3i(0,1,0)));
        BlockState down = getCommandBlock(currentPos.offset(new Vec3i(0,-1,0)));
        BlockState north = getCommandBlock(currentPos.offset(new Vec3i(0,0,-1)));
        BlockState south = getCommandBlock(currentPos.offset(new Vec3i(0,0,1)));
        BlockState east = getCommandBlock(currentPos.offset(new Vec3i(1,0,0)));
        BlockState west = getCommandBlock(currentPos.offset(new Vec3i(-1,0,0)));

        switch(currentDir){
            case UP -> {
                if(up != null) next = up;
            }
            case DOWN -> {
                if(down != null) next = down;
            }
            case NORTH -> {
                if(north != null) next = north;
            }
            case SOUTH -> {
                if(south != null) next = south;
            }
            case EAST -> {
                if(east != null) next = east;
            }
            case WEST -> {
                if(west != null) next = west;
            }
        }

        if((up != null) && up.getValue(CommandBlock.FACING) == Direction.DOWN) prior.add(new Pair<>(up, Direction.UP));
        if((down != null) && down.getValue(CommandBlock.FACING) == Direction.UP) prior.add(new Pair<>(down, Direction.DOWN));
        if((north != null) && north.getValue(CommandBlock.FACING) == Direction.SOUTH) prior.add(new Pair<>(north, Direction.NORTH));
        if((south != null) && south.getValue(CommandBlock.FACING) == Direction.NORTH) prior.add(new Pair<>(south, Direction.SOUTH));
        if((east != null) && east.getValue(CommandBlock.FACING) == Direction.WEST) prior.add(new Pair<>(east, Direction.EAST));
        if((west != null) && west.getValue(CommandBlock.FACING) == Direction.EAST) prior.add(new Pair<>(west, Direction.WEST));

        if(!prior.isEmpty()){
            prior.sort((o1, o2) -> {
                if(o1.getA().getValue(CommandBlock.FACING) == currentDir) return -1;
                if(o2.getA().getValue(CommandBlock.FACING) == currentDir) return 1;
                return 0;
            });
        }
    }

    public boolean isInChain(){
        return (next != null && next.is(Blocks.CHAIN_COMMAND_BLOCK))
                || (!prior.isEmpty() && current.is(Blocks.CHAIN_COMMAND_BLOCK));
    }

    public BlockState getNext(){
        return next;
    }

    public List<Pair<BlockState, Direction>> getPrior(){
        return prior;
    }

    private BlockState getCommandBlock(BlockPos pos){
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            return world.getBlockState(pos);
        }
        return null;
    }
}
