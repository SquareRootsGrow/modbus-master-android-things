# modbus-master-android-things
A master Modbus client for Android Things


### Example usage
```
val modbusMasterClient = ModbusMasterClient()
val request = ModbusRequest.ReadInputRegisters(
            slaveId = 0x01,
            inputAddress = 0x03,
            numInputsToRead = 6
    )
val response: ReadInputRegisterResponse = modbusMasterClient.readInputRegisters(request)

when (response) {
    is ModbusResponse.Success -> {
        val responseData: ModbusData.ReadInputRegisters = response.modbusData
        val registerValues: ShortArray = responseData.data
        ...
    }
    is ModbusResponse.Error -> {
        val error: ModbusError = response.error
        // handle the error appropriately
        ...
    }
}
```
