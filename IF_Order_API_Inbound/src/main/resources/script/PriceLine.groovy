import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import java.math.RoundingMode

class BusinessException extends Exception {
    int status
    BusinessException(String m, int s) { super(m); status = s }
}

def Message processData(Message message) {
    def p = new JsonSlurper().parseText(message.getBody(String))
    if (p.price == null) throw new IllegalStateException("Product response has no price")

    int idx = (message.getProperty("CamelSplitIndex") ?: 0) as int
    String itemNo = ((idx + 1) * 10).toString()

    BigDecimal qty   = new BigDecimal(message.getProperty("qty").toString())
    BigDecimal price = new BigDecimal(p.price.toString())                     // USD
    BigDecimal rate  = new BigDecimal(message.getProperty("fxRate").toString()) // USD -> order currency

    if (p.stock != null && qty > new BigDecimal(p.stock.toString()))
        throw new BusinessException("Product ${p.id} has insufficient stock (requested ${qty.toPlainString()}, available ${p.stock})", 409)

    BigDecimal netUsd = qty.multiply(price).setScale(2, RoundingMode.HALF_UP)
    BigDecimal net    = qty.multiply(price).multiply(rate).setScale(2, RoundingMode.HALF_UP)

    message.setBody("<line>" +
        "<itemNumber>${itemNo}</itemNumber>" +
        "<productId>${XmlUtil.escapeXml(p.id.toString())}</productId>" +
        "<sku>${XmlUtil.escapeXml((p.sku ?: '').toString())}</sku>" +
        "<description>${XmlUtil.escapeXml((p.title ?: '').toString())}</description>" +
        "<quantity>${qty.toPlainString()}</quantity>" +
        "<unitPriceUsd>${price.toPlainString()}</unitPriceUsd>" +
        "<netUsd>${netUsd.toPlainString()}</netUsd>" +
        "<net>${net.toPlainString()}</net></line>")
    return message
}
