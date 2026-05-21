package com.shopapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    // ── ⚠️  CONFIGURE THESE TWO LINES ─────────────────────────────────────────
    // Use a dedicated Gmail account for sending (not your personal account).
    // In Gmail: Settings → Security → Enable 2FA → App Passwords → generate one.
    private const val SENDER_EMAIL    = "henghong477@gmail.com"
    private const val SENDER_APP_PASS = "xxxx xxxx xxxx xxxx" // ← paste your App Password here
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns true only if the developer has filled in real credentials */
    fun isConfigured(): Boolean =
        SENDER_EMAIL != "yourapp@gmail.com" &&
        SENDER_APP_PASS != "xxxx xxxx xxxx xxxx" &&
        SENDER_EMAIL.contains("@") &&
        SENDER_APP_PASS.length >= 16

    /**
     * Sends a password-reset email with the new temp password.
     * Must be called from a background coroutine (uses Dispatchers.IO internally).
     */
    suspend fun sendPasswordReset(
        toEmail: String,
        userName: String,
        tempPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val props = Properties().apply {
                put("mail.smtp.host",            "smtp.gmail.com")
                put("mail.smtp.port",            "587")
                put("mail.smtp.auth",            "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust",       "smtp.gmail.com")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASS)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "ShopApp Support"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "🔐 ShopApp — Password Reset"
                setContent(buildEmailHtml(userName, tempPassword), "text/html; charset=utf-8")
            }

            Transport.send(message)
        }
    }

    private fun buildEmailHtml(name: String, tempPassword: String): String = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
          <div style="max-width:480px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
            <div style="background:#534AB7;padding:24px;text-align:center">
              <h1 style="color:#fff;margin:0;font-size:22px">🛍️ ShopApp</h1>
              <p style="color:#ccc;margin:6px 0 0">Password Reset Request</p>
            </div>
            <div style="padding:28px">
              <p style="color:#333;font-size:15px">Hello <b>$name</b>,</p>
              <p style="color:#555;font-size:14px">
                We received a request to reset your password. 
                Use the temporary password below to login, then change it right away.
              </p>
              <div style="background:#f0eeff;border:2px dashed #534AB7;border-radius:8px;
                          padding:16px;text-align:center;margin:24px 0">
                <p style="margin:0;color:#888;font-size:12px;letter-spacing:1px">TEMPORARY PASSWORD</p>
                <p style="margin:8px 0 0;color:#534AB7;font-size:26px;
                           font-weight:bold;letter-spacing:4px">$tempPassword</p>
              </div>
              <p style="color:#e53935;font-size:13px">
                ⚠️ This password is valid for <b>one login only</b>. 
                Please change it after you log in.
              </p>
              <p style="color:#888;font-size:12px;margin-top:32px">
                If you did not request this, you can safely ignore this email.
              </p>
            </div>
            <div style="background:#f9f9f9;padding:16px;text-align:center">
              <p style="color:#aaa;font-size:11px;margin:0">© ShopApp — Automated message, do not reply</p>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()
}
