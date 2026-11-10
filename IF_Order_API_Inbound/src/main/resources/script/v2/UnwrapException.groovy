// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message

def Message processData(Message message) {
    def ex = message.getProperty("CamelExceptionCaught")
    def root = ex
    int guard = 0
    while (root?.getCause() != null && root.getCause() != root && guard++ != 8) {
        root = root.getCause()
    }
    if (root != null) message.setProperty("CamelExceptionCaught", root)
    return message
}