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

public class CheckoutPage extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(CheckoutPage.class);

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
        LOG.info("CheckoutPage parameters: {}", parameters);

        result = "<!DOCTYPE html>\r\n"
            + "<html lang=\"en\">\r\n"
            + "\t<head>\r\n"
            + "\t\t<style>\r\n"
            + "\t\t\tbody {background: #110e21;font-family: 'Poppins', sans-serif;font-weight: 500;color: #fff;}\r\n"
            + "\t\t\tsection {background: #0b0917;border-bottom: 2px solid #ec00f8;"
            + "box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.19);color: #fff;padding: 1em;position: absolute;top: 50%;"
            + "left: 50%;margin-right: -50%;transform: translate(-50%, -50%);text-align: center;}\r\n"
            + "\t\t\th1 {color: #00f3bd;font-weight: 600;}\r\n"
            + "</style>\r\n"
            + "\t\t<meta charset=\"utf-8\">\r\n"
            + "\t\t<title>" + APP_NAME + " Checkout</title>\r\n"
            + "\t</head>\r\n"
            + "\t<body><section>\r\n"
            + "\t<div>\n"
            + "\t\t<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"89\" style=\"shape-rendering:geometricPrecision;text-rendering:geometricPrecision;image-rendering:optimizeQuality;fill-rule:evenodd;clip-rule:evenodd\"><path fill=\"#fefffe\" d=\"M34.5 6.5h4c-.166 11.005 0 22.005.5 33 3.843-8.475 10.343-11.142 19.5-8l-9 12a249.577 249.577 0 0 1 11.5 15c9.227-13.97 7.394-26.47-5.5-37.5a43.042 43.042 0 0 0-13-4.5v-10c17.309 1.484 28.476 10.484 33.5 27 3.126 16.913-2.207 30.246-16 40a590.031 590.031 0 0 1-21-25 50.441 50.441 0 0 0-.5 10c-3.937.294-7.77-.04-11.5-1l-1-36c-12.397 9.984-14.73 21.984-7 36 8.35 9.718 18.85 13.218 31.5 10.5 3.096 2.029 4.762 4.862 5 8.5-23.413 6.607-39.913-1.06-49.5-23-4.246-24.196 5.254-39.863 28.5-47z\"/><path fill=\"#fbfffe\" d=\"M87.5 23.5h10a64.875 64.875 0 0 0 1 16c4.208-5.964 9.875-7.63 17-5a149.519 149.519 0 0 0-10 10.5 91.866 91.866 0 0 1 10 17.5 31.12 31.12 0 0 1-11-1l-6-9a25.87 25.87 0 0 0-1 10h-10v-39z\"/><path fill=\"#fbfffe\" d=\"M117.5 23.5h10v39h-10v-39z\"/><path fill=\"#fbfffe\" d=\"M163.5 23.5h10a31.117 31.117 0 0 0 1 11c14.587-3.253 20.753 2.747 18.5 18-3.381 9.86-9.714 12.527-19 8-3.149 1.862-6.649 2.53-10.5 2v-39zm13 18c2.544-.396 4.711.271 6.5 2 2.04 8.975-.96 11.642-9 8-1.137-3.963-.303-7.296 2.5-10z\"/><path style=\"opacity:.8\" fill=\"#fbfffe\" d=\"M258.5 24.5h5v5h-5v-5z\"/><path fill=\"#fbfffe\" d=\"M132.5 33.5h9c-.166 6.01.001 12.01.5 18 1.733 2.102 3.733 2.435 6 1 .5-6.325.666-12.658.5-19h10v29a40.939 40.939 0 0 1-9-.5c-1-2-2-2-3 0-4.652 1.193-8.819.36-12.5-2.5a119.884 119.884 0 0 1-1.5-26z\"/><path style=\"opacity:.8\" fill=\"#fbfffe\" d=\"M205.5 34.5a60.94 60.94 0 0 1 11 .5 16.636 16.636 0 0 1 6 5 10.76 10.76 0 0 1-3 2.5c-6.735-5.9-12.569-5.234-17.5 2-1.522 7.63 1.478 12.297 9 14a32.363 32.363 0 0 0 9-4c1.946.263 2.612 1.263 2 3-4.574 4.65-10.074 6.15-16.5 4.5-9.446-6.013-11.28-13.847-5.5-23.5a59.835 59.835 0 0 1 5.5-4z\"/><path style=\"opacity:.8\" fill=\"#fbfffe\" d=\"M233.5 34.5c16.055-.91 21.889 6.424 17.5 22-6.251 6.51-13.418 7.677-21.5 3.5-7.598-10.08-6.265-18.58 4-25.5zm2 4c10.15.304 13.983 5.304 11.5 15-6.043 6.795-11.71 6.462-17-1-1.651-6.14.182-10.806 5.5-14z\"/><path style=\"opacity:.8\" fill=\"#fbfffe\" d=\"M258.5 34.5h5v28h-5v-28z\"/><path style=\"opacity:.8\" fill=\"#fbfffe\" d=\"M270.5 34.5c2.266-.359 4.099.308 5.5 2 4.271-1.996 8.771-2.496 13.5-1.5l3.5 3.5a102.865 102.865 0 0 1 1.5 24h-4c.166-6.01-.001-12.01-.5-18-1.903-6.03-5.737-7.53-11.5-4.5a6.977 6.977 0 0 0-2.5 3.5c-.5 6.325-.666 12.658-.5 19h-5v-28z\"/></svg>\n"
            + "\t</div>"
            + "\t<h1>Checkout</h1>\r\n";
        String message = "<p>Cannot find the order<p/>";
        MoOrder order;

        try {
            boolean isUuid = orderId.startsWith("ord_");
            String orderUuid;
            if (isUuid) {
                orderUuid = orderId.substring(4);
                order = crossStorageApi.find(defaultRepo, orderUuid, MoOrder.class);
            } else {
                order = crossStorageApi.find(defaultRepo, MoOrder.class)
                                       .by("orderNumber", orderId)
                                       .getResult();
                orderUuid = order.getUuid();
            }
            String normalizedId = "ord_" + orderUuid;

            if ("created".equals(order.getStatus())) {
                message = "\t<div>To pay your order, please scan this QR-code<br/> using your " + APP_NAME + " mobile app</div><br/>\r\n"
                    + "\t<canvas id=\"qr-code\"></canvas>\r\n"
                    + "\t<script src=\"https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js\"></script>\r\n"
                    + "\t<script>\r\n"
                    + "\tvar qr;\r\n"
                    + "\t(function() {\r\n"
                    + "\tqr = new QRious({\r\n"
                    + "\telement: document.getElementById('qr-code'),\r\n"
                    + "\tsize: 200,\r\n"
                    + "\tvalue: \"https://link.klubcoin.net/payment/" + normalizedId + "\"});\r\n"
                    + "\tqr.set({foreground: 'black',size: 200});\r\n"
                    + "\t})();\r\n"
                    + "\t</script>\r\n"
                    + "\t<script>\n"
                    + "\t\t(function () {\n"
                    + "\t\t\tconst pathname = window.location.pathname;\n"
                    + "\t\t\tconst context = pathname.substring(0, pathname.indexOf(\"/\", 1));\n"
                    + "\t\t\tconst getPaymentStatus = async (orderId) => {\n"
                    + "\t\t\t\tconst url = window.location.origin + context + \"/rest/pg/v1/paymentStatus/\" + orderId;\n"
                    + "\t\t\t\tconst response = await fetch(url);\n"
                    + "\t\t\t\tconst json = await response.json();\n"
                    + "\t\t\t\treturn json.status;\n"
                    + "\t\t\t};\n"
                    + "\t\t\tconst checkPaymentStatus = async (orderId) => {\n"
                    + "\t\t\t\tconst status = await getPaymentStatus(orderId);\n"
                    + "\t\t\t\tif (status === \"paid\" || status === \"canceled\" || status === \"expired\") {\n"
                    + "\t\t\t\t\tconst validateUrl = window.location.origin + context + \"/rest/validate-payment/\" + orderId;\n"
                    + "\t\t\t\t\tconst validateResponse = await fetch(validateUrl);\n"
                    + "\t\t\t\t\tconst validateResult = await validateResponse.json();\n"
                    + "\t\t\t\t\tif (\"success\" === validateResult.status) {\n"
                    + "\t\t\t\t\t\twindow.location.href = \"" + order.getRedirectUrl() + "\";\n"
                    + "\t\t\t\t\t}\n"
                    + "\t\t\t\t} else {\n"
                    + "\t\t\t\t\tsetTimeout(() => {\n"
                    + "\t\t\t\t\t\tcheckPaymentStatus(\"" + normalizedId + "\");\n"
                    + "\t\t\t\t\t}, 15000);\n"
                    + "\t\t\t\t}\n"
                    + "\t\t\t};\n"
                    + "\t\t\tcheckPaymentStatus(\"" + normalizedId + "\");\n"
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
