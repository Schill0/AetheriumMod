package com.espmod.litematica.automation.pathfinding;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Pathfinder {

    public static class PathNode implements Comparable<PathNode> {
        public BlockPos pos;
        public PathNode parent;
        public double gCost; // Cost from start
        public double hCost; // Heuristic cost to end

        public PathNode(BlockPos pos, PathNode parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }

        public double fCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(PathNode o) {
            return Double.compare(this.fCost(), o.fCost());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            PathNode pathNode = (PathNode) obj;
            return pos.equals(pathNode.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    private static final int MAX_DROP = 3;
    private static final int MAX_ITERATIONS = 2000;

    public static Queue<BlockPos> findPath(MinecraftClient client, BlockPos start, BlockPos target) {
        if (client.world == null)
            return new LinkedList<>();

        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(start, null, 0, start.getSquaredDistance(target));
        openSet.add(startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            PathNode current = openSet.poll();

            // If we are close enough to the target (e.g., adjacent or exactly on it)
            if (current.pos.getSquaredDistance(target) <= 4.0) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighborPos : getNeighbors(client, current.pos)) {
                if (closedSet.contains(neighborPos))
                    continue;

                double newCostToNeighbor = current.gCost + current.pos.getSquaredDistance(neighborPos);
                PathNode neighborNode = new PathNode(neighborPos, current, newCostToNeighbor,
                        neighborPos.getSquaredDistance(target));

                boolean inOpenSet = false;
                for (PathNode n : openSet) {
                    if (n.pos.equals(neighborPos)) {
                        inOpenSet = true;
                        if (newCostToNeighbor < n.gCost) {
                            n.gCost = newCostToNeighbor;
                            n.parent = current;
                        }
                        break;
                    }
                }

                if (!inOpenSet) {
                    openSet.add(neighborNode);
                }
            }
            iterations++;
        }

        return new LinkedList<>(); // No path found or too complex
    }

    private static Queue<BlockPos> retracePath(PathNode endNode) {
        LinkedList<BlockPos> path = new LinkedList<>();
        PathNode current = endNode;
        while (current != null) {
            path.addFirst(current.pos);
            current = current.parent;
        }
        return path;
    }

    private static List<BlockPos> getNeighbors(MinecraftClient client, BlockPos current) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 4 Horizontal directions
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        for (int[] dir : directions) {
            int dx = dir[0];
            int dz = dir[1];

            BlockPos horizontal = current.add(dx, 0, dz);

            if (isPassable(client, horizontal)) {
                // Must have block below to stand on, or drop down
                int dropAmount = 0;
                BlockPos floor = horizontal.down();
                while (dropAmount <= MAX_DROP && isPassable(client, floor)) {
                    dropAmount++;
                    floor = floor.down();
                }

                if (dropAmount <= MAX_DROP && isSafeToStand(client, floor.up())) {
                    neighbors.add(floor.up());
                }
            } else {
                // Try jumping up 1 block
                BlockPos jumpTarget = current.add(dx, 1, dz);
                if (isPassable(client, jumpTarget) && isPassable(client, current.up(2))) { // Headroom check
                    neighbors.add(jumpTarget);
                }
            }
        }
        return neighbors;
    }

    private static boolean isPassable(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        BlockState feet = client.world.getBlockState(pos);
        BlockState head = client.world.getBlockState(pos.up());
        return !feet.isSolidBlock(client.world, pos) && !head.isSolidBlock(client.world, pos.up());
    }

    // Check if the block has solid ground beneath it and no lava
    private static boolean isSafeToStand(MinecraftClient client, BlockPos pos) {
        if (client.world == null)
            return false;
        BlockState feet = client.world.getBlockState(pos);
        BlockState ground = client.world.getBlockState(pos.down());

        if (feet.getFluidState().isStill() || ground.getFluidState().isStill())
            return false;

        return ground.isSolidBlock(client.world, pos.down());
    }
}
