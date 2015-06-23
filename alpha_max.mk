#
# Copyright 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add our custom kernel params before calling parent product
LOCAL_KERNEL_PATH := device/newcircle/alpha_max/kernel/x86_64
TARGET_PREBUILT_KERNEL := $(LOCAL_KERNEL_PATH)/bzImage
TARGET_PREBUILT_KERNEL_MODULE_PATH := $(LOCAL_KERNEL_PATH)/modules
TARGET_PREBUILT_KERNEL_MODULE_TREE_PATH := $(LOCAL_KERNEL_PATH)/lib/modules

# Inherit from the Intel MinnowBoard device target
$(call inherit-product, device/intel/baytrail/minnow_max/minnow_max.mk)

PRODUCT_NAME := alpha_max
PRODUCT_DEVICE := alpha_max
PRODUCT_BRAND := Android
PRODUCT_MODEL := NewCircle Alpha on MinnowBoard
PRODUCT_MANUFACTURER := Intel

# Enable overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Device-specific init files
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/init.alpha_max.rc:root/init.alpha_max.rc \
    $(LOCAL_PATH)/init.recovery.alpha_max.rc:root/init.recovery.alpha_max.rc \
    $(LOCAL_PATH)/ueventd.alpha_max.rc:root/ueventd.alpha_max.rc

# Include all packages from this file
include $(LOCAL_PATH)/packages.mk

# Add our device-specific packages
PRODUCT_PACKAGES += MrknLogNative
PRODUCT_PACKAGES += MrknLogClient

