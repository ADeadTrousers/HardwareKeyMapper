Hardware Key Mapper
=================================================
Change the actions assigned to the hardware keys on your android devices according to the screen orientation.
Set an additional overlay to use in case of a specific foreground app to send an intent instead of the normal key behaviour.

The original idea for this app was to be able to utilize the vendor hardware key on the side of the Atom L and XL devices made by Unihertz.
In the stock rom this button was assigned to one specific task and could not be changed to something else.
It was built upon the app [NavButtonRemap by shuhaowu](https://github.com/shuhaowu/NavButtonRemap) which was also greatly enhanced in the process.

## Integration into AOSP
Add the following to the manifest-tag in your `roomservice.xml`

```xml
  <project name="ADeadTrousers/HardwareKeyMapper" path="vendor/are/HardwareKeyMapper" remote="github" revision="master" />
```

Add the following to your device.mk

```
PRODUCT_PACKAGES += \
    HardwareKeyMapper
```

## Special Thanks to
[NavButtonRemap by shuhaowu](https://github.com/shuhaowu/NavButtonRemap) which was a great inspiration to expand upon.