// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.LocalDate

class ValidationException extends Exception {
    int status
    ValidationException(String m, int s) { super(m); status = s }
}

def Message processData(Message message) {
    // --- correlation id: accept the caller's id only if it is harmless ---
    String corr = message.getHeaders().get("X-Correlation-ID")?.toString()
    if (!(corr ==~ /^[A-Za-z0-9._-]{1,64}$/)) corr = UUID.randomUUID().toString()
    message.setProperty("CorrId", corr)
    message.setProperty("integrationName", "O2C_ORDER_FACADE")
    def log = messageLogFactory.getMessageLog(message)

    // --- body must be a JSON object (streamed, not loaded as one String) ---
    Reader reader = message.getBody(java.io.Reader)
    if (reader == null) throw new ValidationException("Body is empty", 400)
    def json
    try {
        reader.withCloseable { r -> json = new JsonSlurper().parse(r) }
    } catch (Exception e) {
        throw new ValidationException("Body is not valid JSON", 400)
    }
    if (!(json instanceof Map)) throw new ValidationException("Body must be a JSON object", 400)

    // --- header fields ---
    String extId = json.externalOrderId?.toString()
    if (!extId) throw new ValidationException("externalOrderId is required", 400)
    if (!(extId ==~ /^[A-Za-z0-9._-]{1,64}$/))
        throw new ValidationException("externalOrderId may only contain letters, digits, dot, underscore, hyphen (max 64)", 400)

    String cust = json.customerId?.toString()
    if (!cust) throw new ValidationException("customerId is required", 400)
    if (!(cust ==~ /^[0-9]{1,6}$/)) throw new ValidationException("customerId must be a positive integer", 400)

    try { LocalDate.parse(json.orderDate?.toString()) }
    catch (Exception e) { throw new ValidationException("orderDate must use YYYY-MM-DD", 400) }

    String cur = json.currency?.toString()
    if (!(cur in ["EUR", "GBP", "CHF", "CAD"]))
        throw new ValidationException("currency must be one of EUR, GBP, CHF, CAD", 422)

    // --- items ---
    if (!(json.items instanceof List) || json.items.isEmpty()) throw new ValidationException("items must not be empty", 400)
    if (json.items.size() > 20) throw new ValidationException("items must not contain more than 20 entries", 422)
    json.items.eachWithIndex { item, i ->
        if (!(item instanceof Map)) throw new ValidationException("items[${i}] must be an object", 422)
        String pid = item.productId?.toString()
        if (!pid) throw new ValidationException("items[${i}].productId is required", 422)
        if (!(pid ==~ /^[0-9]{1,6}$/)) throw new ValidationException("items[${i}].productId must be a positive integer", 422)
        BigDecimal q
        try { q = new BigDecimal(item.quantity.toString()) }
        catch (Exception e) { throw new ValidationException("items[${i}].quantity must be a number", 422) }
        if (q <= 0 || q.stripTrailingZeros().scale() > 0)
            throw new ValidationException("items[${i}].quantity must be a positive integer", 422)
    }

    // --- properties for later steps ---
    message.setProperty("businessKey", extId)
    message.setProperty("customerId", cust)
    message.setProperty("orderDate", json.orderDate.toString())
    message.setProperty("orderCurrency", cur)
    message.setProperty("itemCount", json.items.size().toString())
    // the stream is consumed, so rebuild the JSON from the validated object
    message.setProperty("OrigJson", JsonOutput.toJson(json))
    if (log) {
        log.addCustomHeaderProperty("CorrId", corr)
        log.addCustomHeaderProperty("BusinessKey", extId)
    }
    return message
}