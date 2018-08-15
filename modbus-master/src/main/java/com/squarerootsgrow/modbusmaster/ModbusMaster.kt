package com.squarerootsgrow.modbusmaster

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import com.google.android.things.pio.UartDevice.FLUSH_IN_OUT


private const val READ_BUFFER_SIZE = 100
private const val POST_SEND_DELAY = 10.toLong()
private const val BUFFER_READ_DELAY = 15.toLong()

/**
 * Modbus Master Client
 *
 * @param uartDeviceName the UART device name, defaulted to "MINIUART".
 *
 * @param txControlPin the GPIO pin name if you're using a protocol which requires a separate pin to control transmission/reception
 * (such as RS-485), otherwise leave null. Defaulted to null.
 *
 * @param baudRate the baud rate of the UART connection, defaulted to 9600.
 *
 * @param dataSize the UART data size in bits, defaulted to 8 bits.
 *
 * @param parity the UART parity mode, one of [UartDevice.PARITY_NONE], [UartDevice.PARITY_EVEN], [UartDevice.PARITY_ODD]
 * , [UartDevice.PARITY_MARK], [UartDevice.PARITY_SPACE]. Defaulted to, [UartDevice.PARITY_NONE]
 *
 * @param stopBits the UART stop bits, defaulted to 1
 *
 * @param timeoutMillis the UART response timeout, in milliseconds, defaulted to 1000 milliseconds
 */
class ModbusMaster(
        uartDeviceName: String = "MINIUART",
        txControlPinName: String? = null,
        baudRate: Int = 9600,
        dataSize: Int = 8,
        parity: Int = UartDevice.PARITY_NONE,
        stopBits: Int = 1,
        private val timeoutMillis: Int = 1000
) {
    private val uartDevice: UartDevice
    private var txControlPin: Gpio? = null

    init {
        val manager = PeripheralManager.getInstance()
        uartDevice = manager.openUartDevice(uartDeviceName).apply {
            setBaudrate(baudRate)
            setDataSize(dataSize)
            setParity(parity)
            setStopBits(stopBits)
        }

        txControlPin = txControlPinName?.let { txControlPin ->
            manager.openGpio(txControlPin).apply {
                setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                setActiveType(Gpio.ACTIVE_HIGH)
            }
        }
    }

    fun readCoils(request: ModbusRequest.ReadCoils): ReadCoilsResponse = send(request)
    fun readDiscreteInputs(request: ModbusRequest.ReadDiscreteInputs): ReadDiscreteInputsResponse = send(request)
    fun readInputRegisters(request: ModbusRequest.ReadInputRegisters): ReadInputRegisterResponse = send(request)
    fun readHoldingRegisters(request: ModbusRequest.ReadHoldingRegisters): ReadHoldingRegisterResponse = send(request)
    fun writeCoil(request: ModbusRequest.WriteCoil): WriteCoilResponse = send(request)
    fun writeCoils(request: ModbusRequest.WriteCoils): WriteCoilsResponse = send(request)
    fun writeHoldingRegister(request: ModbusRequest.WriteHoldingRegister): WriteHoldingRegisterResponse = send(request)
    fun writeHoldingRegisters(request: ModbusRequest.WriteHoldingRegisters): WriteHoldingRegistersResponse = send(request)


    private fun <T : ModbusData> send(request: ModbusRequest<T>): ModbusResponse<T> {
        val modbusMessage: ByteArray = request.constructMessage()
        txControlPin?.value = true
        uartDevice.write(modbusMessage, modbusMessage.size)
        Thread.sleep(POST_SEND_DELAY)
        txControlPin?.value = false

        return try {
            request.parseResponse(readResponse())
        } catch (e: Exception) {
            ModbusResponse.Error(ModbusError.Exception(e))
        }
    }

    private fun readResponse(): ByteArray {
        var totalBytesRead = 0
        var buffer = ByteArray(0)
        val timestampStart = System.currentTimeMillis()

        while (System.currentTimeMillis() < timestampStart + timeoutMillis) {
            var tempBuffer = ByteArray(READ_BUFFER_SIZE)
            var bytesRead = uartDevice.read(tempBuffer, tempBuffer.size)
            while (bytesRead > 0) {
                // copy into main buffer
                buffer += tempBuffer.copyOf(bytesRead)
                totalBytesRead += bytesRead

                // check if there is more data coming down the pipe
                Thread.sleep(BUFFER_READ_DELAY)
                tempBuffer = ByteArray(READ_BUFFER_SIZE)
                bytesRead = uartDevice.read(tempBuffer, tempBuffer.size)
            }

            if (totalBytesRead > 0) {
                break
            }
        }
        return buffer.copyOf(totalBytesRead)
    }
}