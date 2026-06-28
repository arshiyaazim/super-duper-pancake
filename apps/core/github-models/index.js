import WhatsAppConnector from "./connector.js";

const connector = new WhatsAppConnector();
connector.start().catch(console.error);