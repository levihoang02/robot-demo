package com.example.test.UART

import com.hoho.android.usbserial.driver.UsbSerialPort

data class UsbSerialConfig(

    val baudRate: Int = 115200,

    val dataBits: Int = 8,

    val stopBits: Int =
        UsbSerialPort.STOPBITS_1,

    val parity: Int =
        UsbSerialPort.PARITY_NONE,

    val readTimeout: Int = 1000,

    val writeTimeout: Int = 1000,

    val reconnectDelayMs: Long = 2000,

    val maxRetryCount: Int = 5
)