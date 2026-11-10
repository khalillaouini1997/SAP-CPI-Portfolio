import com.sap.gateway.ip.core.customdev.util.Message
def Message processData(Message message) {
    def stored = message.getBody(String)
    message.setProperty("IsDuplicate", (stored != null && stored.trim()) ? "true" : "false")
    return message
}
