// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message

def Message processData(Message message) {
    def body = message.getBody(String)
    int rows = (body =~ /<Row>/).count
    message.setProperty("RowCount", rows)
    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        log.addCustomHeaderProperty("RowCount", rows.toString())
        log.addCustomHeaderProperty("CorrId", message.getProperty("RunDate").toString())
    }
    return message
}
