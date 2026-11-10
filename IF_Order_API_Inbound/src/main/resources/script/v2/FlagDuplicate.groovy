// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message

def Message processData(Message message) {
    String stored = message.getBody(String)?.trim()
    // A real hit is the marker written by Marker_Body: <SalesOrderNo>|<CorrId>.
    // If the entry is missing, the Get step leaves the original request JSON in the body.
    boolean found = (stored != null) && (stored ==~ /^[A-Za-z0-9]+\|[A-Za-z0-9._-]+$/)
    message.setProperty("IsDuplicate", found ? "true" : "false")
    return message
}