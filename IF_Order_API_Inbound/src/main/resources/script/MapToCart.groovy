import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {
    // stream the canonical order JSON instead of loading it as one String
    Reader reader = message.getBody(java.io.Reader)
    if (reader == null) throw new IllegalStateException("Canonical order body is empty")
    def c
    reader.withCloseable { r -> c = new JsonSlurper().parse(r) }

    def cart = [
        userId  : new BigDecimal(c.soldToParty.customerId.toString()).intValueExact(),
        products: c.items.collect { [id: new BigDecimal(it.productId.toString()).intValueExact(),
                                     quantity: new BigDecimal(it.quantity.toString()).intValueExact()] }
    ]
    message.setBody(JsonOutput.toJson(cart))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")
    return message
}