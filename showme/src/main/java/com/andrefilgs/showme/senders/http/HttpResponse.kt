package com.andrefilgs.showme.senders.http

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class HttpResponse(

    var success: Boolean?=null,
    var responseCode: String? = null,

    var url: String? = null,
    var method: String? = null,
    var bodyResponse: String? = null,
    var bodySent: String? = null,
    var headers: HashMap<String,String?>? = hashMapOf()


):Parcelable {}