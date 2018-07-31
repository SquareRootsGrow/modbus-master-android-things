package com.squarerootsgrow.modbusmaster

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * all test requests/responses coming from http://www.simplymodbus.ca/FAQ.htm
 *
 */
class ModbusRequestTest {

    private fun <T : ModbusData> testRequestMessage(request: ModbusRequest<T>, expectedRequest: ByteArray) {
        assertArrayEquals(expectedRequest, request.constructMessage())
    }

    private fun <T : ModbusData> testResponseParsingSuccess(request: ModbusRequest<T>, response: ByteArray): ModbusResponse<T> {
        val actualRequest = request.parseResponse(response)
        assertEquals(ModbusResponse.Success::class.java, actualRequest::class.java)
        return actualRequest
    }

    private val readCoilsRequest = ModbusRequest.ReadCoils(0x11, 0x13, 0x25)
    @Test
    fun readCoilsRequestMessage() {
        testRequestMessage(
                readCoilsRequest,
                byteArrayOf(
                        0x11,
                        0x01,
                        0x00,
                        0x13,
                        0x00,
                        0x25,
                        0x0E,
                        0x84.toByte()
                )
        )
    }

    @Test
    fun readCoilsResponseParsing() {
        val actualResponse = testResponseParsingSuccess(
                readCoilsRequest,
                byteArrayOf(
                        0x11,
                        0x01,
                        0x05,
                        0xCD.toByte(),
                        0x6B,
                        0xB2.toByte(),
                        0x0E,
                        0x1B,
                        0x45,
                        0xE6.toByte()
                )
        )

        val data = (actualResponse as ModbusResponse.Success).modbusData.data
        assertArrayEquals(booleanArrayOf(
                // 1100 1101
                true,
                false,
                true,
                true,
                false,
                false,
                true,
                true,
                // 0110 1011
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                // 1011 0010
                false,
                true,
                false,
                false,
                true,
                true,
                false,
                true,
                // 0000 1110
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                // 0001 1011 (3 space holders)
                true,
                true,
                false,
                true,
                true
        ), data)
    }

    private val readDiscreteInputsRequest = ModbusRequest.ReadDiscreteInputs(0x11, 0xC4, 0x16)
    @Test
    fun readDiscreteInputsMessage() {
        testRequestMessage(
                readDiscreteInputsRequest,
                byteArrayOf(
                        0x11,
                        0x02,
                        0x00,
                        0xC4.toByte(),
                        0x00,
                        0x16,
                        0xBA.toByte(),
                        0xA9.toByte()
                )
        )
    }

    @Test
    fun readDiscreteInputsResponseParsing() {
        val actualResponse = testResponseParsingSuccess(
                readDiscreteInputsRequest,
                byteArrayOf(
                        0x11,
                        0x02,
                        0x03,
                        0xAC.toByte(),
                        0xDB.toByte(),
                        0x35,
                        0x20,
                        0x18
                )
        )

        val data = (actualResponse as ModbusResponse.Success).modbusData.data

        assertArrayEquals(
                booleanArrayOf(
                        // 1010 1100
                        false,
                        false,
                        true,
                        true,
                        false,
                        true,
                        false,
                        true,
                        // 1101 1011
                        true,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        true,
                        // 0011 0101 (2 space holders
                        true,
                        false,
                        true,
                        false,
                        true,
                        true
                ),
                data
        )

    }

    private val readInputRegistersRequest = ModbusRequest.ReadInputRegisters(0x11, 0x08, 0x01)
    @Test
    fun readInputRegistersMessage() {
        testRequestMessage(
                readInputRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x04,
                        0x00,
                        0x08,
                        0x00,
                        0x01,
                        0xB2.toByte(),
                        0x98.toByte()
                )
        )
    }

    @Test
    fun readInputRegistersResponseParsing() {
        val actualResponse = testResponseParsingSuccess(
                readInputRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x04,
                        0x02,
                        0x00,
                        0x0A,
                        0xF8.toByte(),
                        0xF4.toByte()
                )
        )

        val data = (actualResponse as ModbusResponse.Success).modbusData.data

        assertArrayEquals(
                shortArrayOf(
                    0x000A
                ),
                data
        )
    }

    private val readHoldingRegistersRequest = ModbusRequest.ReadHoldingRegisters(0x11, 0x6B, 0x03)
    @Test
    fun readHoldingRegistersMessage() {
        testRequestMessage(
                readHoldingRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x03,
                        0x00,
                        0x6B,
                        0x00,
                        0x03,
                        0x76,
                        0x87.toByte()
                )
        )
    }

    @Test
    fun readHoldingRegistersResponseParsing() {
        val actualResponse = testResponseParsingSuccess(
                readHoldingRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x03,
                        0x06,
                        0xAE.toByte(),
                        0x41,
                        0x56,
                        0x52,
                        0x43,
                        0x40,
                        0x49,
                        0xAD.toByte()
                )
        )

        val data = (actualResponse as ModbusResponse.Success).modbusData.data
        assertArrayEquals(
                shortArrayOf(
                        0xAE41.toShort(),
                        0x5652,
                        0x4340
                ),
                data
        )
    }

    private val writeCoilRequestTrue = ModbusRequest.WriteCoil(0x11, 0xAC, true)
    private val writeCoilRequestFalse = ModbusRequest.WriteCoil(0x11, 0xAC, false)
    @Test
    fun writeCoilRequestMessage() {
        testRequestMessage(
                writeCoilRequestTrue,
                byteArrayOf(
                        0x11,
                        0x05,
                        0x00,
                        0xAC.toByte(),
                        0xFF.toByte(),
                        0x00,
                        0x4E,
                        0x8B.toByte()
                )
        )


        testRequestMessage(
                writeCoilRequestFalse,
                addCrc(
                        byteArrayOf(
                                0x11,
                                0x05,
                                0x00,
                                0xAC.toByte(),
                                0x00,
                                0x00
                        )
                )
        )
    }

    @Test
    fun writeCoilResponseParsing() {
        testResponseParsingSuccess(
                writeCoilRequestTrue,
                byteArrayOf(
                        0x11,
                        ModbusFunctionCodes.WRITE_SINGLE_COIL,
                        0x00,
                        0xAC.toByte(),
                        0xFF.toByte(), //true
                        0x00,
                        0x4E,
                        0x8B.toByte()
                )
        )

        testResponseParsingSuccess(
                writeCoilRequestFalse,
                addCrc(
                        byteArrayOf(
                                0x11,
                                ModbusFunctionCodes.WRITE_SINGLE_COIL,
                                0x00,
                                0xAC.toByte(),
                                0x00, //false
                                0x00
                        )
                )
        )
    }

    private val writeCoilsRequest = ModbusRequest.WriteCoils(
            0x11,
            0x13,
            0x0A,
            booleanArrayOf(
                    true,
                    false,
                    true,
                    true,
                    false,
                    false,
                    true,
                    true,
                    true,
                    false
            )
    )

    @Test
    fun writeCoilsRequestMessage() {
        testRequestMessage(
                writeCoilsRequest,
                byteArrayOf(
                        0x11,
                        0x0F,
                        0x00,
                        0x13,
                        0x00,
                        0x0A,
                        0x02,
                        0xCD.toByte(),
                        0x01,
                        0xBF.toByte(),
                        0x0B
                )
        )
    }

    @Test
    fun writeCoilsResponseParsing() {
        testResponseParsingSuccess(
                writeCoilsRequest,
                byteArrayOf(
                        0x11,
                        0x0F,
                        0x00,
                        0x13,
                        0x00,
                        0x0A,
                        0x26,
                        0x99.toByte()
                )
        )
    }

    private val writeHoldingRegisterRequest = ModbusRequest.WriteHoldingRegister(0x11, 0x01, 0x03)

    @Test
    fun writeHoldingRegisterMessage() {
        testRequestMessage(
                writeHoldingRegisterRequest,
                byteArrayOf(
                        0x11,
                        0x06,
                        0x00,
                        0x01,
                        0x00,
                        0x03,
                        0x9A.toByte(),
                        0x9B.toByte()
                )
        )
    }

    @Test
    fun writeHoldingRegisterResponseParsing() {
        testResponseParsingSuccess(
                writeHoldingRegisterRequest,
                byteArrayOf(
                        0x11,
                        0x06,
                        0x00,
                        0x01,
                        0x00,
                        0x03,
                        0x9A.toByte(),
                        0x9B.toByte()
                )
        )
    }

    private val writeHoldingRegistersRequest = ModbusRequest.WriteHoldingRegisters(0x11, 0x01, 0x02, shortArrayOf(0x0A, 0x0102))

    @Test
    fun writeHoldingRegistersMessage() {
        testRequestMessage(
                writeHoldingRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x10,
                        0x00,
                        0x01,
                        0x00,
                        0x02,
                        0x04,
                        0x00,
                        0x0A,
                        0x01,
                        0x02,
                        0xC6.toByte(),
                        0xF0.toByte()
                )
        )
    }

    @Test
    fun writeHoldingRegistersResponseParsing() {
        testResponseParsingSuccess(
                writeHoldingRegistersRequest,
                byteArrayOf(
                        0x11,
                        0x10,
                        0x00,
                        0x01,
                        0x00,
                        0x02,
                        0x12,
                        0x98.toByte()
                )
        )
    }
}