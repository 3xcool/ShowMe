package com.example.showme.senders.api

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class HttpResponse(

    var responseCode: String? = null,

    var url: String? = null,
    var method: String? = null,
    var bodyResponse: String? = null,
    var bodySent: String? = null,
    var headers: HashMap<String,String?>? = hashMapOf()


):Parcelable {}