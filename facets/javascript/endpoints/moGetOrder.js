import EndpointInterface from "#{API_BASE_URL}/api/rest/endpoint/EndpointInterface.js";

// the request schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this is used to validate and parse the request parameters
const requestSchema = {
  "title" : "moGetOrderRequest",
  "id" : "moGetOrderRequest",
  "default" : "Schema definition for moGetOrder",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object"
}

// the response schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this could be used to parse the result
const responseSchema = {
  "title" : "moGetOrderResponse",
  "id" : "moGetOrderResponse",
  "default" : "Schema definition for moGetOrder",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "order" : {
      "title" : "Order",
      "description" : "Order",
      "id" : "MoOrder",
      "storages" : [ "SQL" ],
      "type" : "object",
      "properties" : {
        "amount" : {
          "title" : "MoOrder.amount",
          "description" : "Amount",
          "id" : "CE_MoOrder_amount",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "number"
        },
        "consumerDateOfBirth" : {
          "title" : "MoOrder.consumerDateOfBirth",
          "description" : "date of birth",
          "id" : "CE_MoOrder_consumerDateOfBirth",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "format" : "date-time"
        },
        "metadata" : {
          "title" : "MoOrder.metadata",
          "description" : "metadata",
          "id" : "CE_MoOrder_metadata",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string"
        },
        "quantity_shipped" : {
          "title" : "MoOrder.quantity_shipped",
          "description" : "Quantity Shipped",
          "id" : "CE_MoOrder_quantity_shipped",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "number"
        },
        "orderNumber" : {
          "title" : "MoOrder.orderNumber",
          "description" : "Order number",
          "id" : "CE_MoOrder_orderNumber",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 50
        },
        "redirectUrl" : {
          "title" : "MoOrder.redirectUrl",
          "description" : "redirectUrl",
          "id" : "CE_MoOrder_redirectUrl",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 1000
        },
        "method" : {
          "title" : "MoOrder.method",
          "description" : "payment method",
          "id" : "CE_MoOrder_method",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 20
        },
        "amountCaptured" : {
          "title" : "MoOrder.amountCaptured",
          "description" : "Amount captured",
          "id" : "CE_MoOrder_amountCaptured",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "number"
        },
        "creationDate" : {
          "title" : "MoOrder.creationDate",
          "description" : "creation date",
          "id" : "CE_MoOrder_creationDate",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "format" : "date-time"
        },
        "locale" : {
          "title" : "MoOrder.locale",
          "description" : "locale",
          "id" : "CE_MoOrder_locale",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 5
        },
        "assignedTo" : {
          "title" : "MoOrder.assignedTo",
          "description" : "Assigned to",
          "id" : "CE_MoOrder_assignedTo",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 50
        },
        "expiresAt" : {
          "title" : "MoOrder.expiresAt",
          "description" : "Expires at",
          "id" : "CE_MoOrder_expiresAt",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "format" : "date-time"
        },
        "webhookUrl" : {
          "title" : "MoOrder.webhookUrl",
          "description" : "webhookUrl",
          "id" : "CE_MoOrder_webhookUrl",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 1000
        },
        "shopperCountryMustMatchBillingCountry" : {
          "title" : "MoOrder.shopperCountryMustMatchBillingCountry",
          "description" : "Shop & bill country must match",
          "id" : "CE_MoOrder_shopperCountryMustMatchBillingCountry",
          "storages" : [ "SQL" ],
          "default" : "false",
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string"
        },
        "amountRefunded" : {
          "title" : "MoOrder.amountRefunded",
          "description" : "Amount refunded",
          "id" : "CE_MoOrder_amountRefunded",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "number"
        },
        "shippingAddress" : {
          "title" : "MoOrder.shippingAddress",
          "description" : "Shipping address",
          "id" : "CE_MoOrder_shippingAddress",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "$ref" : "#/definitions/MoAddress"
        },
        "currency" : {
          "title" : "MoOrder.currency",
          "description" : "currency",
          "id" : "CE_MoOrder_currency",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 3
        },
        "payment" : {
          "title" : "MoOrder.payment",
          "description" : "payment properties",
          "id" : "CE_MoOrder_payment",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string"
        },
        "billingAddress" : {
          "title" : "MoOrder.billingAddress",
          "description" : "Billing Address",
          "id" : "CE_MoOrder_billingAddress",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "$ref" : "#/definitions/MoAddress"
        },
        "lines" : {
          "title" : "MoOrder.lines",
          "description" : "lines",
          "id" : "CE_MoOrder_lines",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "array",
          "uniqueItems" : true,
          "items" : {
            "title" : "MoOrder.lines item",
            "id" : "CE_MoOrder_lines_item",
            "$ref" : "#/definitions/MoOrderLine"
          }
        },
        "email" : {
          "title" : "MoOrder.email",
          "description" : "Customer email",
          "id" : "CE_MoOrder_email",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "type" : "string",
          "maxLength" : 500
        },
        "group" : {
          "title" : "MoOrder.group",
          "description" : "Assigned user group",
          "id" : "CE_MoOrder_group",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "enum" : [ "TECH_TEAM", "SUP_TEAM", "SALES_TEAM" ]
        },
        "status" : {
          "title" : "MoOrder.status",
          "description" : "status",
          "id" : "CE_MoOrder_status",
          "storages" : [ "SQL" ],
          "nullable" : true,
          "readOnly" : false,
          "versionable" : false,
          "enum" : [ "canceled", "expired", "shipping", "created", "authorized", "pending", "paid", "completed" ]
        }
      }
    }
  }
}

// should contain offline mock data, make sure it adheres to the response schema
const mockResult = {};

class moGetOrder extends EndpointInterface {
	constructor() {
		// name and http method, these are inserted when code is generated
		super("moGetOrder", "GET");
		this.requestSchema = requestSchema;
		this.responseSchema = responseSchema;
		this.mockResult = mockResult;
	}

	getRequestSchema() {
		return this.requestSchema;
	}

	getResponseSchema() {
		return this.responseSchema;
	}
}

export default new moGetOrder();