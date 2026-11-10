import com.sap.gateway.ip.core.customdev.util.Message

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