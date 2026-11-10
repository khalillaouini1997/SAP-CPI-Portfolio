// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
import groovy.json.JsonOutput
import groovy.xml.XmlSlurper

class BusinessException extends Exception {
    int status
    BusinessException(String m, int s) { super(m); status = s }
}

def Message processData(Message message) {
    // body = gathered XML (multimap:Messages) containing one <line> per item
    def xml   = new XmlSlurper().parseText(message.getBody(String))
    def lines = xml.'**'.findAll { it.name() == 'line' }

    int expected = (message.getProperty("itemCount") ?: "0").toString() as int
    if (lines.size() != expected)
        throw new IllegalStateException("Expected ${expected} priced lines but got ${lines.size()}")

    BigDecimal total    = lines.collect { new BigDecimal(it.net.text()) }.inject(BigDecimal.ZERO) { a, b -> a + b }
    BigDecimal totalUsd = lines.collect { new BigDecimal(it.netUsd.text()) }.inject(BigDecimal.ZERO) { a, b -> a + b }

    // business rule: order-value limit in the ORDER currency (externalized as MAX_ORDER_VALUE)
    BigDecimal limit = new BigDecimal(message.getProperty("maxOrderValue").toString())
    String cur = message.getProperty("orderCurrency")
    if (total > limit)
        throw new BusinessException("Order value exceeds the allowed limit of ${limit.setScale(2).toPlainString()} ${cur}", 409)

    def out = [
        externalOrderId  : message.getProperty("businessKey"),
        orderDate        : message.getProperty("orderDate"),
        currency         : cur,
        fxRate           : message.getProperty("fxRate"),
        fxRateDate       : message.getProperty("fxDate"),
        soldToParty      : [customerId: message.getProperty("soldTo"),
                            name      : message.getProperty("custName"),
                            shipTo    : [street: message.getProperty("shipStreet"), city: message.getProperty("shipCity"),
                                         postalCode: message.getProperty("shipPostal"), country: message.getProperty("shipCountry")]],
        items            : lines.collect { [itemNumber: it.itemNumber.text(), productId: it.productId.text(), sku: it.sku.text(),
                                            description: it.description.text(), quantity: it.quantity.text(),
                                            unitPriceUsd: it.unitPriceUsd.text(), netAmountUsd: it.netUsd.text(),
                                            netAmount: it.net.text()] },
        orderNetAmountUsd: totalUsd.setScale(2).toPlainString(),
        orderNetAmount   : total.setScale(2).toPlainString()
    ]
    message.setProperty("UsdTotal", totalUsd.setScale(2).toPlainString())
    message.setProperty("OrderTotal", total.setScale(2).toPlainString())
    message.setBody(JsonOutput.toJson(out))
    message.setHeader("Content-Type", "application/json")
    return message
}