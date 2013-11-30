USB_flashlight_Glass
====================

App for my own usb flashlight accessory

This uses USB OTG on Google Glass. In XE11 GDK does not have permission to use USB host by default.  
<feature name="android.hardware.usb.host"/>     needs to be added in /etc/permissions/hardware.xml 
Reference: http://stackoverflow.com/questions/11183792/android-usb-host-and-hidden-devices


