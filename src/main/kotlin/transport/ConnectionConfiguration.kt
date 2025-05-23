package com.labeq.transport

interface ConnectionConfiguration {
    /**
     * Describes the type of connection used by the device.
     *
     * This property specifies the mode of communication, such as network, serial,
     * USB, SSH, or a custom-defined connection type. It is used to configure and
     * manage the appropriate connection settings in the implementation.
     */
    val connectionType: DeviceConnectionType
}