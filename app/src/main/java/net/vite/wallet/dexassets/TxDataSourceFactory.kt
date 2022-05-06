package net.vite.wallet.dexassets

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource

class DexTxDataSourceFactory(val addr: String, val tti: String, val isViteX:Boolean) : DataSource.Factory<String, DexTokenTxVo>() {
    val sourceLiveData = MutableLiveData<DexTxListPagedDataSource>()
    override fun create(): DataSource<String, DexTokenTxVo> {
        val source = DexTxListPagedDataSource(addr, tti, isViteX)
        sourceLiveData.postValue(source)
        return source
    }
}