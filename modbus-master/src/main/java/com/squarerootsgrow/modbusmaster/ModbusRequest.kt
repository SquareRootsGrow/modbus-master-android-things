package com.squarerootsgrow.modbusmaster

import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.READ_COILS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.READ_DISCRETE_INPUTS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.READ_HOLDING_REGISTERS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.READ_INPUT_REGISTERS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.WRITE_MULTIPLE_COILS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.WRITE_MULTIPLE_HOLDING_REGISTERS
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.WRITE_SINGLE_COIL
import com.squarerootsgrow.modbusmaster.ModbusFunctionCodes.WRITE_SINGLE_HOLDING_REGISTER
import kotlin.experimental.and

sealed class ModbusRequest<T : ModbusData>(val slaveId: Byte, val funcCode: Byte, val address: Short) {

    /**
     * Constructs a Modbus request
     */
    internal abstract fun constructMessage(): ByteArray

    /**
     * Request-specific parsing
     */
    protected abstract fun parseData(data: ByteArray): ModbusResponse<T>

    /**
     * General Response Error handling/parsing
     */
    internal fun parseResponse(response: ByteArray): ModbusResponse<T> {
        val responseLength = response.size

        // if we have no data then we timed out
        if (responseLength == 0) return ModbusResponse.Error(ModbusError.Timeout())

        // must be at least two bytes long for slaveId and funcCode
        if (responseLength < 2) return ModbusResponse.Error(ModbusError.IncompleteResponse())

        // Check Slave ID
        if (response[0] != slaveId) return ModbusResponse.Error(ModbusError.IncorrectSlaveId())

        // check Func Code (only check the first 7 bits as the last bit holds a potential error)
        if ((response[1] and 0x7F) != funcCode) return ModbusResponse.Error(ModbusError.IncorrectFuncCode())

        // check for slave error (if the 8th bit is set to 1)
        // TODO parse specific errors from slave
        if (response[1].toInt() ushr 7 and 0x01 == 0x01) return ModbusResponse.Error(ModbusError.InternalSlaveError())

        // check CRC
        val crc = Word(loByte = response[responseLength - 2], hiByte = response[responseLength - 1])
        val calculatedCrc = calculateCrc(response.copyOf(responseLength - 2)) //remove CRC before calculating

        if (crc != calculatedCrc) return ModbusResponse.Error(ModbusError.CrcError())

        if (ModbusFunctionCodes.isReadRequest(funcCode)) {
            val numDataBytes: Byte = response[2]
            val totalExpectedDataBytes = numDataBytes + 5 //[1 byte slaveId, 1 byte funcCode, 1 byte numDataBytes, 2 bytes CRC]

            if (responseLength != totalExpectedDataBytes) return ModbusResponse.Error(ModbusError.IncompleteResponse())

            //actual response data starts at response[3]
            return parseData(response.sliceArray(3 until 3 + numDataBytes))
        } else {
            // ensure that the dataAddress written matches the intended address
            val dataAddress: Short = Word(hiByte = response[2], loByte = response[3]).toShort()

            if (dataAddress != address) return ModbusResponse.Error(ModbusError.IncorrectDataAddress())

            return parseData(response.sliceArray(4 until 6)) // Write response data is only ever 2 bytes long
        }
    }

    abstract class AbstractReadRequest<T : ModbusData>(
            slaveId: Byte,
            funcCode: Byte,
            address: Short,
            val numAddressesToRead: Short
    ) : ModbusRequest<T>(slaveId, funcCode, address) {
        override fun constructMessage(): ByteArray {
            val addressBytes = address.toWord()
            val numToReadBytes = numAddressesToRead.toWord()
            return addCrc(
                    byteArrayOf(
                            slaveId,
                            funcCode,
                            addressBytes.hiByte,
                            addressBytes.loByte,
                            numToReadBytes.hiByte,
                            numToReadBytes.loByte
                    )
            )
        }
    }

    class ReadCoils(
            slaveId: Byte,
            coilAddress: Short,
            numCoilsToRead: Short
    ) : AbstractReadRequest<ModbusData.ReadCoils>(slaveId, READ_COILS, coilAddress, numCoilsToRead) {

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.ReadCoils> {
            // each coil value is one bit, 1 byte holds up to 8 coil values
            val expectedNumBytes = Math.ceil(numAddressesToRead.toDouble() / 8).toInt()
            return if (expectedNumBytes != data.size) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.ReadCoils(numAddressesToRead, data))
            }
        }
    }

    class ReadDiscreteInputs(
            slaveId: Byte,
            inputAddress: Short,
            numInputsToRead: Short
    ) : AbstractReadRequest<ModbusData.ReadDiscreteInputs>(slaveId, READ_DISCRETE_INPUTS, inputAddress, numInputsToRead) {

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.ReadDiscreteInputs> {
            // each coil value is one bit, 1 byte holds up to 8 coil values
            val expectedNumBytes = Math.ceil(numAddressesToRead.toDouble() / 8).toInt()
            return if (expectedNumBytes != data.size) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.ReadDiscreteInputs(numAddressesToRead, data))
            }
        }
    }

    class ReadInputRegisters(
            slaveId: Byte,
            inputAddress: Short,
            numInputsToRead: Short
    ) : AbstractReadRequest<ModbusData.ReadInputRegisters>(slaveId, READ_INPUT_REGISTERS, inputAddress, numInputsToRead) {

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.ReadInputRegisters> {
            // Data should be 2 bytes per address
            return if (data.size != numAddressesToRead * 2) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.ReadInputRegisters(data))
            }
        }
    }

    class ReadHoldingRegisters(
            slaveId: Byte,
            registerAddress: Short,
            numAddressesToRead: Short
    ) : AbstractReadRequest<ModbusData.ReadHoldingRegisters>(slaveId, READ_HOLDING_REGISTERS, registerAddress, numAddressesToRead) {

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.ReadHoldingRegisters> {
            // Data should be 2 bytes per address
            return if (data.size != numAddressesToRead * 2) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.ReadHoldingRegisters(data))
            }
        }
    }

    class WriteCoil(
            slaveId: Byte,
            coilAddress: Short,
            private val state: Boolean
    ) : ModbusRequest<ModbusData.WriteCoil>(slaveId, WRITE_SINGLE_COIL, coilAddress) {

        override fun constructMessage(): ByteArray {
            val coilAddressBytes = address.toWord()

            return addCrc(
                    byteArrayOf(
                            slaveId,
                            WRITE_SINGLE_COIL,
                            coilAddressBytes.hiByte,
                            coilAddressBytes.loByte,
                            (if (state) 0xFF else 0x00).toByte(),
                            0x00
                    )
            )
        }

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.WriteCoil> {
            val stateWritten: Boolean = Word(hiByte = data[0], loByte = data[1]).toShort() == 0xFF00.toShort()
            return if (stateWritten != state) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.WriteCoil())
            }
        }
    }

    class WriteCoils(
            slaveId: Byte,
            firstCoilAddress: Short,
            private val numCoils: Short,
            private val coilStates: BooleanArray
    ) : ModbusRequest<ModbusData.WriteCoils>(slaveId, WRITE_MULTIPLE_COILS, firstCoilAddress) {

        override fun constructMessage(): ByteArray {
            if (numCoils.toInt() != coilStates.size) {
                throw IllegalStateException("coilStates array length must equal the number of coils being written!")
            }

            val coilStateBitMask = coilStates.toBitMaskByteArray()
            val numBytesData: Byte = coilStateBitMask.size.toByte()
            val firstCoilAddressBytes = address.toWord()
            val numCoilsBytes = numCoils.toWord()

            val message = byteArrayOf(
                    slaveId,
                    ModbusFunctionCodes.WRITE_MULTIPLE_COILS,
                    firstCoilAddressBytes.hiByte,
                    firstCoilAddressBytes.loByte,
                    numCoilsBytes.hiByte,
                    numCoilsBytes.loByte,
                    numBytesData
            )

            return addCrc(message + coilStateBitMask)
        }

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.WriteCoils> {
            val numCoilsWritten = Word(hiByte = data[0], loByte = data[1]).toShort()
            return if (numCoilsWritten != numCoils) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.WriteCoils())
            }
        }
    }

    class WriteHoldingRegister(
            slaveId: Byte,
            registerAddress: Short,
            private val value: Short
    ) : ModbusRequest<ModbusData.WriteHoldingRegister>(slaveId, WRITE_SINGLE_HOLDING_REGISTER, registerAddress) {
        override fun constructMessage(): ByteArray {
            val registerAddressBytes = address.toWord()
            val valueBytes = value.toWord()
            return addCrc(
                    byteArrayOf(
                            slaveId,
                            WRITE_SINGLE_HOLDING_REGISTER,
                            registerAddressBytes.hiByte,
                            registerAddressBytes.loByte,
                            valueBytes.hiByte,
                            valueBytes.loByte
                    )
            )
        }

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.WriteHoldingRegister> {
            val valueWritten = Word(hiByte = data[0], loByte = data[1]).toShort()
            return if (valueWritten != value) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.WriteHoldingRegister())
            }
        }
    }

    class WriteHoldingRegisters(
            slaveId: Byte,
            firstRegisterAddress: Short,
            private val numRegisters: Short,
            private val data: ShortArray
    ) : ModbusRequest<ModbusData.WriteHoldingRegisters>(slaveId, WRITE_MULTIPLE_HOLDING_REGISTERS, firstRegisterAddress) {
        override fun constructMessage(): ByteArray {
            if (numRegisters.toInt() != data.size) {
                throw IllegalStateException("data array length must equal the number of registers being written!")
            }

            val numBytesData: Byte = (numRegisters * 2).toByte() // two bytes per register value
            val firstCoilAddressBytes = address.toWord()
            val numCoilsBytes = numRegisters.toWord()

            val message = byteArrayOf(
                    slaveId,
                    ModbusFunctionCodes.WRITE_MULTIPLE_HOLDING_REGISTERS,
                    firstCoilAddressBytes.hiByte,
                    firstCoilAddressBytes.loByte,
                    numCoilsBytes.hiByte,
                    numCoilsBytes.loByte,
                    numBytesData
            )

            val dataByteArray: ByteArray = data.flatMap { regvalue ->
                regvalue.toWord().let {
                    byteArrayOf(it.hiByte, it.loByte).asIterable()
                }
            }.toByteArray()

            return addCrc(message + dataByteArray)
        }

        override fun parseData(data: ByteArray): ModbusResponse<ModbusData.WriteHoldingRegisters> {
            val numRegistersWritten = Word(hiByte = data[0], loByte = data[1]).toShort()
            return if (numRegistersWritten != numRegisters) {
                ModbusResponse.Error(ModbusError.IncorrectData())
            } else {
                ModbusResponse.Success(ModbusData.WriteHoldingRegisters())
            }
        }
    }
}