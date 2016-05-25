package repose.core

import farseek.core._
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex

/** Core mod class for Repose.
  * @see [[repose.ReposeMod]] for non-core mod class.
  * @author delvr
  */
@SortingIndex(value = FarseekCoreModSortIndex + 1)
class ReposeCoreMod extends FarseekBaseCoreMod {

    protected val transformerClasses = Seq(classOf[ReposeClassTransformer])
}

/** [[MethodReplacementTransformer]] for Repose. Extends collision detection for slopes and update methods for falling blocks.
  * @author delvr
  */
class ReposeClassTransformer extends MethodReplacementTransformer {

    implicit private val transformer = this

    protected val methodReplacements = Seq(
        MethodReplacement("net/minecraft/block/Block", "getCollisionBoundingBox", "func_180640_a",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/util/AxisAlignedBB;",
            "repose/block/SlopingBlockExtensions/getCollisionBoundingBox"),

        MethodReplacement("net/minecraft/block/Block", "addCollisionBoxesToList", "func_180638_a",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;)V",
            "repose/block/SlopingBlockExtensions/addCollisionBoxesToList"),

        MethodReplacement("net/minecraft/entity/Entity", "isEntityInsideOpaqueBlock", "func_70094_T",
            "()Z",
            "repose/block/SlopingBlockExtensions/isEntityInsideOpaqueBlock"),

        MethodReplacement("net/minecraft/block/Block", "onBlockAdded", "func_176213_c",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;)V",
            "repose/block/FallingBlockExtensions/onBlockAdded"),

        MethodReplacement("net/minecraft/block/Block", "onBlockPlacedBy", "func_180633_a",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V",
            "repose/block/FallingBlockExtensions/onBlockPlacedBy"),

        MethodReplacement("net/minecraft/block/Block", "onNeighborBlockChange", "func_176204_a",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/Block;)V",
            "repose/block/FallingBlockExtensions/onNeighborBlockChange"),

        MethodReplacement("net/minecraft/block/Block", "updateTick", "func_180650_b",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V",
            "repose/block/FallingBlockExtensions/updateTick"),

        MethodReplacement("net/minecraft/block/Block", "onBlockDestroyedByPlayer", "func_176206_d",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;)V",
            "repose/block/FallingBlockExtensions/onBlockDestroyedByPlayer"),

        MethodReplacement("net/minecraft/block/BlockFalling", "canFallInto", "func_180685_d",
            "(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;)Z",
            "repose/block/FallingBlockExtensions/canFallInto"),

        MethodReplacement("net/minecraft/entity/item/EntityFallingBlock", "onUpdate", "func_70071_h_",
            "()V",
            "repose/entity/item/EntityFallingBlockExtensions/onUpdate")
    )
}