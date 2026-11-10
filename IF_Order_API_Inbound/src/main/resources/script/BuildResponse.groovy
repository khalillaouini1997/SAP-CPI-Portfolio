import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {
    def corr = message.getProperty("CorrId")
    message.setHeader("CamelHttpResponseCode", 201)
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-Correlation-ID", corr)
    message.setBody(JsonOutput.toJson([
        salesOrder    : message.getProperty("SalesOrderNo"),
        correlationId : corr,
        currency      : message.getProperty("orderCurrency"),
        orderNetAmount: message.getProperty("OrderTotal"),
        fxRate        : message.getProperty("fxRate"),
        fxRateDate    : message.getProperty("fxDate")]))
    return message
}
