// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
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
