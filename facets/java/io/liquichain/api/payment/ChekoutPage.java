package io.liquichain.api.payment;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.customEntities.MoOrder;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ChekoutPage extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ChekoutPage.class);

    @Inject
    private CrossStorageApi crossStorageApi;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;
    private Repository defaultRepo;

    private String APP_NAME;

    private String result;
    private String orderId;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getResult() {
        return result;
    }

    private void init() {
        defaultRepo = repositoryService.findDefaultRepository();
        ParamBean config = paramBeanFactory.getInstance();
        APP_NAME = config.getProperty("app.name", "Liquichain");
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        result = "<!DOCTYPE html>\r\n"
            + "<html lang=\"en\">\r\n"
            + "\t<head>\r\n"
            + "\t\t<style>body {background: white }\r\n"
            + "\t\t\tsection {background: #ac17e3;color: white;border-radius: 1em;padding: 1em;position: absolute;top: 50%;"
            + "left: 50%;margin-right: -50%;transform: translate(-50%, -50%); text-align: center  }</style>\r\n"
            + "\t\t<meta charset=\"utf-8\">\r\n"
            + "\t\t<title>" + APP_NAME + " Checkout</title>\r\n"
            + "\t</head>\r\n"
            + "\t<body><section>\r\n"
            + "\t<h1>Checkout</h1>\r\n";
        String message = "<p>Cannot find the order<p/>";
        MoOrder order;
        try {
            orderId = orderId.startsWith("ord_") ? orderId.substring(4) : orderId;
            order = crossStorageApi.find(defaultRepo, orderId, MoOrder.class);
            if ("created".equals(order.getStatus())) {
                message =
                    "\t<h3>To pay your order, please scan this QR-code<br/> using your " + APP_NAME + " mobile app</h3><br/>\r\n"
                        + "\t<canvas id=\"qr-code\"></canvas>\r\n"
                        + "\t<script src=\"https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js\"></script>\r\n"
                        + "\t<script>\r\n"
                        + "\tvar qr;\r\n"
                        + "\t(function() {\r\n"
                        + "\tqr = new QRious({\r\n"
                        + "\telement: document.getElementById('qr-code'),\r\n"
                        + "\tsize: 200,\r\n"
                        + "\tvalue: 'ord_" + orderId + "'});\r\n"
                        + "\tqr.set({foreground: 'black',size: 200});\r\n"
                        + "\t})();\r\n"
                        + "\t</script>\r\n";
                message += "\t<script>\n"
                    + "\t\t(function() {\n"
                    + "\t\t\tconst getPaymentStatus = async (orderId) => {\n"
                    + "\t\t\t\tconst url = window.location.origin + \"/rest/pg/v1/paymentStatus/\" + orderId;\n"
                    + "\t\t\t\tconst response = await fetch(url);\n"
                    + "\t\t\t\treturn response.json();\n"
                    + "\t\t}\n\n"
                    + "\t\t\tconst checkPaymentStatus = async (orderId) => {\n"
                    + "\t\t\t\tconst response = getPayment(orderId);\n"
                    + "\t\t\t\tif(response.status === \"paid\"){\n"
                    + "\t\t\t\t\twindow.location.href = \"" + order.getRedirectUrl() + "\";\n"
                    + "\t\t\t\t} else {\n"
                    + "\t\t\t\t\tsetTimeout(()=> { checkPaymentStatus(" + orderId + ") }, 4000);\n"
                    + "\t\t\t}\n"
                    + "\t\t}\n\n"
                    + "\t\t\tcheckPaymentStatus(id);\n"
                    + "\t\t})();\n"
                    + "\t</script>";
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
