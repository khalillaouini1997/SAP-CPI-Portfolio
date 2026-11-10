import com.sap.gateway.ip.core.customdev.util.Message
def Message processData(Message message) {
    def ex = message.getProperty("CamelExceptionCaught")
    if (ex?.getClass()?.getCanonicalName() == "org.apache.camel.component.ahc.AhcOperationFailedException") {
        int s = message.getProperty("ErrStatus") as int
        message.setProperty("ErrText", s == 404 ? "Referenced customer or product was not found" : "Upstream system returned ${s}".toString())
    }
    return message
}
