// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
import groovy.json.JsonSlurper
import java.math.RoundingMode

def Message processData(Message message) {
    def resp = null
    List<String> notes = []

    // stream the backend reply instead of loading it as one String
    try {
        Reader reader = message.getBody(java.io.Reader)
        if (reader != null) {
            reader.withCloseable { r -> resp = new JsonSlurper().parse(r) }
        } else {
            notes << "Backend reply body is empty"
        }
    } catch (Exception e) {
        // only the exception type is recorded, never the payload
        notes << ("Backend reply not readable as JSON: " + e.getClass().getSimpleName())
    }

    String no = resp?.id?.toString() ?: "UNKNOWN"
    message.setProperty("SalesOrderNo", no)

    // reconciliation: our USD total (sum of rounded USD lines) vs the backend's own total
    String recon = "UNKNOWN"
    try {
        BigDecimal backend = new BigDecimal(resp.total.toString()).setScale(2, RoundingMode.HALF_UP)
        BigDecimal ours    = new BigDecimal(message.getProperty("UsdTotal").toString())
        recon = (backend.compareTo(ours) == 0) ? "MATCH" : "MISMATCH"
    } catch (Exception e) {
        notes << ("Reconciliation not possible: " + e.getClass().getSimpleName())
    }
    message.setProperty("Reconcile", recon)

    String note = notes.join("; ")
    if (note) message.setProperty("ExtractNote", note)

    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        log.addCustomHeaderProperty("SalesOrderNo", no)
        log.addCustomHeaderProperty("Reconcile", recon)
        if (note) log.addCustomHeaderProperty("ExtractNote", note)
    }
    return message
}