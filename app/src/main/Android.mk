LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_RESOURCE_DIR = $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := HardwareKeyMapper

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.core_core \
    androidx.appcompat_appcompat \
    com.google.android.material_material \
    androidx-constraintlayout_constraintlayout \
    androidx.preference_preference

LOCAL_CERTIFICATE := platform
LOCAL_MODULE_TAGS := optional
LOCAL_PRIVILEGED_MODULE := false
LOCAL_USE_AAPT2 := true

LOCAL_PROGUARD_ENABLED := full

LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_MIN_SDK_VERSION := 29
LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)