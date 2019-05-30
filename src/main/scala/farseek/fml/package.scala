package farseek

import net.minecraftforge.fml.{DefaultModContainers, ModLoadingContext, ModLoadingStage}

package object fml {

  /** Throws an [[IllegalStateException]] if the current mod hasn't passed loading stage `stage`, or is in `ERROR` stage.
    * This is mainly intended as a guard for `lazy val`s that risk being initialized too early due to programmer error.
    * Keep in mind that some mod loading stages are synchronous and others run in parallel.
    * @see https://mcforge.readthedocs.io/en/1.13.x/conventions/loadstages/ */
  def assertPassedStage(stage: ModLoadingStage): Unit = {
    val container = ModLoadingContext.get.getActiveContainer
    if(container != DefaultModContainers.MINECRAFT) {
      val modId = container.getModId
      val currentState = container.getCurrentState
      if(currentState == ModLoadingStage.ERROR)
        throw new IllegalStateException(s"Mod $modId is in ERROR stage")
      else if(currentState.compareTo(stage) <= 0)
        throw new IllegalStateException(s"Mod $modId is in $currentState stage, needs to be after $stage stage")
    }
  }

}
