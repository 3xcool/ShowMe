package com.example.showme.senders.api.model


import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

//Simulating some user Object
@Parcelize
class UserAPIModel(

    @SerializedName("Url") @Expose var url: String? = null,

    @SerializedName("Payload") @Expose var payload: String? = null,

    @SerializedName("Message") @Expose var message: String? = null


) : Parcelable {


}

