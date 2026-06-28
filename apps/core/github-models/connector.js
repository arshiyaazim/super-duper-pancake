import { makeWASocket, useMultiFileAuthState, fetchLatestBaileysVersion } from "@whiskeysockets/baileys";
import WhatsAppService from "./service.js";

class WhatsAppConnector {
  constructor() {
    this.service = new WhatsAppService();
    this.sock = null;
  }

  async start() {
    const { version } = await fetchLatestBaileysVersion();
    const { state, saveCreds } = await useMultiFileAuthState("auth_info_baileys");

    this.sock = makeWASocket({
      version,
      auth: state,
      printQRInTerminal: true,
      markOnlineOnConnect: true
    });

    this.sock.ev.on("messages.upsert", async (m) => {
      const msg = m.messages[0];
      if (!msg.message || msg.key.fromMe) return;

      const sender = msg.key.remoteJid;
      const text = msg.message.conversation || msg.message.extendedTextMessage?.text;

      if (text) {
        await this.service.processIncomingMessage(sender, text, new Date(msg.messageTimestamp * 1000));
      }
    });

    this.sock.ev.on("creds.update", saveCreds);
  }
}

export default WhatsAppConnector;