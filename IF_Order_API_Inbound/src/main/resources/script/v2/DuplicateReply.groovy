// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
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
