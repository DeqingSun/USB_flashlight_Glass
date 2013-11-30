/*
 * File: HardwareProfile.h
 */

#ifndef HARDWAREPROFILE_H
#define HARDWAREPROFILE_H

#ifdef __cplusplus
extern "C" {
#endif

#define DEMO_BOARD USER_DEFINED_BOARD
#define USE_INTERNAL_OSC
#define tris_self_power TRISAbits.TRISA2 // Input
#define self_power 0

#define tris_usb_bus_sense TRISAbits.TRISA1 // Input
#define USB_BUS_SENSE 1

#ifdef __cplusplus
}
#endif

#endif /* HARDWAREPROFILE_H */