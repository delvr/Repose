package farseek.util

import org.apache.logging.log4j.{LogManager, Logger}

trait Logging {
  private val log: Logger = LogManager.getLogger

  def debug(msg: => Any): Unit = if(log.isDebugEnabled) log.debug(msg)
  def info (msg: => Any): Unit = if(log.isInfoEnabled ) log.info (msg)
  def warn (msg: => Any): Unit = if(log.isWarnEnabled ) log.warn (msg)
  def error(msg: => Any): Unit = if(log.isErrorEnabled) log.error(msg)
  def fatal(msg: => Any): Unit = if(log.isFatalEnabled) log.fatal(msg)
}
