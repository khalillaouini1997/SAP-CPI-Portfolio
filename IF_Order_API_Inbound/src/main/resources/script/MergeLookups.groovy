import com.sap.gateway.ip.core.customdev.util.Message

class BusinessException extends Exception {
    int status
    BusinessException(String m, int s) { super(m); status = s }
}

def Message processData(Message message) {
    // stream the gathered XML instead of loading it as one String
    Reader reader = message.getBody(java.io.Reader)
    if (reader == null) throw new IllegalStateException("Gathered body is empty")
    def xml
    reader.withCloseable { r -> xml = new XmlSlurper().parse(r) }

    // The gathered body contains two <root> elements, one per branch (order not guaranteed).
    def roots = xml.'**'.findAll { it.name() == 'root' }
    def cust  = roots.find { it.firstName.size() > 0 }
    def fx    = roots.find { it.rates.size() > 0 }

    if (cust == null) throw new BusinessException("Customer data incomplete", 409)
    if (fx == null)   throw new IllegalStateException("Exchange-rate response missing")

    String cur = message.getProperty("orderCurrency")
    String rateText = fx.rates.'*'.find { it.name() == cur }?.text()
    if (!rateText) throw new IllegalStateException("No exchange rate for " + cur)
    BigDecimal rate = new BigDecimal(rateText)
    if (rate <= 0) throw new IllegalStateException("Invalid exchange rate")

    message.setProperty("fxRate", rate.toPlainString())
    message.setProperty("fxDate", fx.date.text())
    message.setProperty("soldTo", cust.id.text())
    message.setProperty("custName", cust.firstName.text() + " " + cust.lastName.text())
    message.setProperty("shipStreet", cust.address.address.text())
    message.setProperty("shipCity", cust.address.city.text())
    message.setProperty("shipPostal", cust.address.postalCode.text())
    message.setProperty("shipCountry", cust.address.country.text())

    def log = messageLogFactory.getMessageLog(message)
    if (log) log.addCustomHeaderProperty("FxRate", rate.toPlainString())
    return message
}