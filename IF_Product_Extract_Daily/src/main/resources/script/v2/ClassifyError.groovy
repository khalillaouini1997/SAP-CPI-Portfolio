// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message

def Message processData(Message message) {
    def ex = message.getProperty("CamelExceptionCaught")
    String category = "TECHNICAL"
    int status = 500
    boolean retryable = true
    String cname = ex?.getClass()?.getCanonicalName()
    if (ex instanceof IllegalArgumentException) {
        category = "VALIDATION"; status = 400; retryable = false
    } else if (ex != null && ex.hasProperty("status")) {          // custom exceptions that carry a status
        status = ex.status
        category = (status in [400, 422]) ? "VALIDATION" : "BUSINESS"; retryable = false
    } else if (cname == "com.sap.it.script.v2.api.exceptions.HttpOperationFailedException") {
        status = ex.getStatusCode()
        if (status in [401, 403]) { category = "SECURITY"; retryable = false }
        else if (status in [408, 429] || status >= 500) { category = "TECHNICAL"; retryable = true }
        else { category = "BUSINESS"; retryable = false }
    } else if (ex?.getMessage()?.toLowerCase()?.contains("mapping")) {
        category = "MAPPING"; status = 500; retryable = false
    }
    message.setProperty("ErrCategory", category)
    message.setProperty("ErrStatus", status)
    message.setProperty("ErrRetryable", retryable.toString())
    message.setProperty("ErrText", (category in ["TECHNICAL", "SECURITY"]) ?
        "Processing failed (${category})" : (ex?.getMessage() ?: "Error"))
    def log = messageLogFactory.getMessageLog(message)
    if (log) { log.addCustomHeaderProperty("ErrCategory", category)
               log.addCustomHeaderProperty("ErrRetryable", retryable.toString()) }
    return message
}
