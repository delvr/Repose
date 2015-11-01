package repose.core

import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex
import farseek.core._

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
        MethodReplacement("net/minecraft/block/Block", "getCollisionBoundingBoxFromPool", "func_149668_a",
            "(Lnet/minecraft/world/World;III)Lnet/minecraft/util/AxisAlignedBB;",
            "repose/block/SlopingBlockExtensions/getCollisionBoundingBoxFromPool"),

        MethodReplacement("net/minecraft/block/Block", "addCollisionBoxesToList", "func_149743_a",
            "(Lnet/minecraft/world/World;IIILnet/minecraft/util/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;)V",
            "repose/block/SlopingBlockExtensions/addCollisionBoxesToList"),

        MethodReplacement("net/minecraft/entity/Entity", "isEntityInsideOpaqueBlock", "func_70094_T",
            "()Z",
            "repose/block/SlopingBlockExtensions/isEntityInsideOpaqueBlock"),

        MethodReplacement("net/minecraft/block/Block", "onBlockAdded", "func_149726_b",
            "(Lnet/minecraft/world/World;III)V",
            "repose/block/FallingBlockExtensions/onBlockAdded"),

        MethodReplacement("net/minecraft/block/Block", "onPostBlockPlaced", "func_149714_e",
            "(Lnet/minecraft/world/World;IIII)V",
            "repose/block/FallingBlockExtensions/onPostBlockPlaced"),

        MethodReplacement("net/minecraft/block/Block", "onNeighborBlockChange", "func_149695_a",
            "(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V",
            "repose/block/FallingBlockExtensions/onNeighborBlockChange"),

        MethodReplacement("net/minecraft/block/Block", "updateTick", "func_149674_a",
            "(Lnet/minecraft/world/World;IIILjava/util/Random;)V",
            "repose/block/FallingBlockExtensions/updateTick"),

        MethodReplacement("net/minecraft/block/Block", "onBlockDestroyedByPlayer", "func_149664_b",
            "(Lnet/minecraft/world/World;IIII)V",
            "repose/block/FallingBlockExtensions/onBlockDestroyedByPlayer"),

        MethodReplacement("net/minecraft/block/BlockFalling", "func_149831_e",
            "(Lnet/minecraft/world/World;III)Z",
            "repose/block/FallingBlockExtensions/canFallThrough"),

        MethodReplacement("net/minecraft/entity/item/EntityFallingBlock", "onUpdate", "func_70071_h_",
            "()V",
            "repose/entity/item/EntityFallingBlockExtensions/onUpdate")
    )
}
