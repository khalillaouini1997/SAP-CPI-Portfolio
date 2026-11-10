import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import java.math.RoundingMode

def Message processData(Message message) {
    def resp = null
    // stream the backend reply instead of loading it as one String
    try {
        Reader reader = message.getBody(java.io.Reader)
        if (reader != null) {
            reader.withCloseable { r -> resp = new JsonSlurper().parse(r) }
        }
    } catch (Exception e) { }

    String no = resp?.id?.toString() ?: "UNKNOWN"
    message.setProperty("SalesOrderNo", no)

    // reconciliation: our USD total (sum of rounded USD lines) vs the backend's own total
    String recon = "UNKNOWN"
    try {
        BigDecimal backend = new BigDecimal(resp.total.toString()).setScale(2, RoundingMode.HALF_UP)
        BigDecimal ours    = new BigDecimal(message.getProperty("UsdTotal").toString())
        recon = (backend.compareTo(ours) == 0) ? "MATCH" : "MISMATCH"
    } catch (Exception e) { }
    message.setProperty("Reconcile", recon)

    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        log.addCustomHeaderProperty("SalesOrderNo", no)
        log.addCustomHeaderProperty("Reconcile", recon)
    }
    return message
}