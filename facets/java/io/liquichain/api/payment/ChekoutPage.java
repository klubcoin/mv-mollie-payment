package io.liquichain.api.payment;

import org.meveo.service.script.Script;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChekoutPage extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ChekoutPage.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String result;
    private String orderId;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResult() {
        return result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        result = "<!DOCTYPE html>\r\n"
            + "<html lang=\"en\">\r\n"
            + "\t<head>\r\n"
            + "\t\t<style>body {background: white }\r\n"
            + "\t\t\tsection {background: #ac17e3;color: white;border-radius: 1em;padding: 1em;position: absolute;top: 50%;"
            + "left: 50%;margin-right: -50%;transform: translate(-50%, -50%); text-align: center  }</style>\r\n"
            + "\t\t<meta charset=\"utf-8\">\r\n"
            + "\t\t<title>Liquichain Checkout</title>\r\n"
            + "\t</head>\r\n"
            + "\t<body><section>\r\n"
            + "\t<h1>Checkout</h1>\r\n";
        String message = "<p>Cannot find the order<p/>";
        MoOrder order = null;
        try {
            order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
            if ("created".equals(order.getStatus())) {
                message =
                    "\t<h3>To pay your order, please scan this QR-code<br/> using your liquichain mobile app</h3><br/>\r\n"
                        + "\t<canvas id=\"qr-code\"></canvas>\r\n"
                        + "\t<div><button class='qr-btn' onclick='location.replace(\"" + order.getRedirectUrl() + "\");'>Continue</button></div>\r\n"
                        + "\t<script src=\"https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js\"></script>\r\n"
                        + "\t<script>\r\n"
                        + "\tvar qr;\r\n"
                        + "\t(function() {\r\n"
                        + "\tqr = new QRious({\r\n"
                        + "\telement: document.getElementById('qr-code'),\r\n"
                        + "\tsize: 200,\r\n"
                        + "\tvalue: 'lcn_ord:" + orderId + "'});\r\n"
                        + "\tvar qrtext = document.getElementById(\"qr-text\").value;\r\n"
                        + "\tqr.set({foreground: 'black',size: 200,value: qrtext});\r\n"
                        + "\t})();\r\n"
                        + "\t</script>\r\n";
            } else {
                message = "<p>Invalid order<p/>";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result += message
            + "\t</section></body>\r\n"
            + "\t</html>\r\n";
    }
}
