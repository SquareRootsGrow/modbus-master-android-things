package com.squarerootsgrow.modbusmaster


sealed class ModbusResponse<T : ModbusData> {
    class Success<T : ModbusData>(val modbusData: T) : ModbusResponse<T>()

    class Error<T : ModbusData>(val error: ModbusError) : ModbusResponse<T>()
}

typealias ReadCoilsResponse = ModbusResponse<ModbusData.ReadCoils>
typealias ReadDiscreteInputsResponse = ModbusResponse<ModbusData.ReadDiscreteInputs>
typealias ReadInputRegisterResponse = ModbusResponse<ModbusData.ReadInputRegisters>
typealias ReadHoldingRegisterResponse = ModbusResponse<ModbusData.ReadHoldingRegisters>
typealias WriteCoilResponse = ModbusResponse<ModbusData.WriteCoil>
typealias WriteCoilsResponse = ModbusResponse<ModbusData.WriteCoils>
typealias WriteHoldingRegisterResponse = ModbusResponse<ModbusData.WriteHoldingRegister>
typealias WriteHoldingRegistersResponse = ModbusResponse<ModbusData.WriteHoldingRegisters>

sealed class ModbusData {

    class ReadCoils(numCoilsRead: Short, data: ByteArray) : ModbusData() {
        val data: BooleanArray = data.bitMaskToBooleanArray(numCoilsRead.toInt())
    }

    class ReadDiscreteInputs(numInputsRead: Short, data: ByteArray) : ModbusData() {
        val data: BooleanArray = data.bitMaskToBooleanArray(numInputsRead.toInt())
    }

    class ReadInputRegisters(data: ByteArray) : ModbusData() {
        val data: ShortArray = data.toShortArray()
    }

    class ReadHoldingRegisters(data: ByteArray) : ModbusData() {
        val data = data.toShortArray()
    }

    class WriteCoil : ModbusData()

    class WriteCoils : ModbusData()

    class WriteHoldingRegister : ModbusData()

    class WriteHoldingRegisters : ModbusData()
}

sealed class ModbusError {
    class Timeout : ModbusError()

    class IncompleteResponse : ModbusError()

    class IncorrectSlaveId : ModbusError()

    class IncorrectFuncCode : ModbusError()

    class IncorrectDataAddress : ModbusError()

    class CrcError : ModbusError()

    class IncorrectData : ModbusError()

    class InternalSlaveError : ModbusError()

    class Exception(val e: kotlin.Exception) : ModbusError()
}