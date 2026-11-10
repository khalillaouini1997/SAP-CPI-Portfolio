import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {
    int status = message.getProperty("ErrStatus") as int
    message.setHeader("CamelHttpResponseCode", status)
    message.setHeader("Content-Type", "application/json")
    message.setBody(JsonOutput.toJson([
        correlationId: message.getProperty("CorrId") ?: message.getProperty("businessKey"),
        integration  : message.getProperty("integrationName") ?: "UNKNOWN",
        errorCategory: message.getProperty("ErrCategory"),
        errorCode    : "${message.getProperty('ErrCategory')}_${status}".toString(),
        message      : message.getProperty("ErrText"),
        retryable    : message.getProperty("ErrRetryable") == "true"
    ]))
    return message
}
