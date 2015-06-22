#ifndef _MRKNLOG_INTERFACE_H
#define _MRKNLOG_INTERFACE_H

#include <stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <hardware/hardware.h>
#include <log/log.h>
#include <log/logger.h>   

__BEGIN_DECLS

#define MRKNLOG_HARDWARE_MODULE_ID "mrknlog"

struct mrknlog_device_t {
  /* 
   * Common HAL device structure.
   * Always placed at the head of the module definition.
   */
  struct hw_device_t common;
  
  /*
   * Android liblog reference to the sub-log(s) we
   * want to access from this module.
   */
  struct logger_list *logger_list;

  /*
   * Flush the log device
   * 
   * Returns: 0 on success, error code on failure
   */
  int (*flush_log)(struct mrknlog_device_t* dev);
  
  /*
   * Get the total log size
   * 
   * Returns: total log size, < 0 on failure
   */
  int (*get_total_log_size)(struct mrknlog_device_t* dev);

  /*
   * Get the used log size
   * 
   * Returns: used log size, < 0 on failure
   */
  int (*get_used_log_size)(struct mrknlog_device_t* dev);
};

__END_DECLS

#endif /* End of the _MRKNLOG_INTERFACE_H block */
