package com.doordash.doordashlite.model

interface IResponseListener<T> {

    fun onSuccess(obj: T?);
    fun onError(message: String?)
}