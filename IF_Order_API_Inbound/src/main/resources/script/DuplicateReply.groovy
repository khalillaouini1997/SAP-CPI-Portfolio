import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
def Message processData(Message message) {
    message.setHeader("CamelHttpResponseCode", 409)
    message.setHeader("Content-Type", "application/json")
    message.setBody(JsonOutput.toJson([
        correlationId: message.getProperty("CorrId"),
        integration  : "O2C_ORDER_FACADE",
        errorCategory: "BUSINESS",
        errorCode    : "DUPLICATE_ORDER",
        message      : "Order ${message.getProperty('businessKey')} was already processed".toString(),
        retryable    : false]))
    return message
}
