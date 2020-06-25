package com.andrefilgs.showme.senders.http.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
class ShowMeHttpLogModel(

    @SerializedName("showMeLog") @Expose var showMeLog: String? = null,
    @SerializedName("timestamp") @Expose var timestamp: Long? = 0L

) : Parcelable {


}

