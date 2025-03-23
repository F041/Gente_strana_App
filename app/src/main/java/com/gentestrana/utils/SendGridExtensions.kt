package com.gentestrana.utils

import com.sendgrid.Request
import com.sendgrid.Method

fun Request.configureForSendGrid() {
    this.method = Method.POST
    this.endpoint = "mail/send"
}