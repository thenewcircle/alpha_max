#define LOG_NDEBUG 1                                               /* <1> */
#define LOG_TAG "MrknLog"                                          /* <1> */
#define LOG_ID LOG_ID_MAIN                                         /* <2> */

#include <mrknlog.h>                                               /* <3> */
#include <fcntl.h>
#include <time.h>
#include <errno.h>
#include <sys/ioctl.h>

static struct logger* get_logger_device(struct logger_list *logger_list) {
  // "open" retrieves and existing logger from the list
  return android_logger_open(logger_list, LOG_ID);                 /* <4> */
}

static int flush_log(struct mrknlog_device_t* dev) {
  SLOGV("Flushing %s", android_log_id_to_name(LOG_ID));            /* <1> */
  
  struct logger *logger = get_logger_device(dev->logger_list);
  return android_logger_clear(logger);                             /* <4> */
}

static int get_total_log_size(struct mrknlog_device_t* dev) {
  SLOGV("Getting total buffer size of %s",                         /* <1> */
      android_log_id_to_name(LOG_ID));
  
  struct logger *logger = get_logger_device(dev->logger_list);
  return android_logger_get_log_size(logger);                      /* <4> */
}

static int get_used_log_size(struct mrknlog_device_t* dev) {
  SLOGV("Getting used buffer size of %s",                          /* <1> */
      android_log_id_to_name(LOG_ID));
  
  struct logger *logger = get_logger_device(dev->logger_list);
  return android_logger_get_log_readable_size(logger);             /* <4> */
}

static int close_mrknlog(struct mrknlog_device_t* dev) {
  SLOGV("Closing %s", android_log_id_to_name(LOG_ID));             /* <1> */

  if (dev) {
    android_logger_list_free(dev->logger_list);                    /* <4> */
    free(dev);                                                     /* <5> */
  }
  return 0;
}

static int open_mrknlog(const struct hw_module_t *module, char const *name,
   struct hw_device_t **device) {

  struct logger_list *logger_read = android_logger_list_open(      /* <4> */
      LOG_ID, O_RDONLY | O_NDELAY, 1, 0);

  if (!logger_read) {
    SLOGE("Failed to open %s: %s",                                 /* <1> */
      android_log_id_to_name(LOG_ID), strerror(errno));
    return -1;
  } else {
    struct mrknlog_device_t *dev =                                 /* <3> */
      malloc(sizeof(struct mrknlog_device_t));                     /* <5> */
    if (!dev) {
      return -ENOMEM;
    }
    SLOGV("Opened %s", android_log_id_to_name(LOG_ID));            /* <1> */
    memset(dev, 0, sizeof(*dev));                                  /* <5> */
    dev->common.tag = HARDWARE_DEVICE_TAG;                         /* <5> */
    dev->common.version = 0;                                       /* <5> */
    dev->common.module = (struct hw_module_t *)module;             /* <5> */
    dev->common.close =                                            /* <6> */
      (int (*)(struct hw_device_t *)) close_mrknlog;
    dev->logger_list = logger_read;
    dev->flush_log = flush_log;                                    /* <6> */
    dev->get_total_log_size = get_total_log_size;                  /* <6> */
    dev->get_used_log_size = get_used_log_size;                    /* <6> */

    *device = (struct hw_device_t *)dev;
    return 0;
  }
}

static struct hw_module_methods_t mrknlog_module_methods = {
  .open = open_mrknlog,                                            /* <6> */
};

struct hw_module_t HAL_MODULE_INFO_SYM = {                         /* <7> */
  .tag = HARDWARE_MODULE_TAG,
  .version_major = 1,
  .version_minor = 0,
  .id = MRKNLOG_HARDWARE_MODULE_ID,                                /* <3> */
  .name = "mrknlog module",
  .author = "NewCircle, Inc.",
  .methods = &mrknlog_module_methods,                              /* <6> */
};
