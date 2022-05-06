package net.vite.wallet.balance.txlist

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.rpc.AccountBlock


class TxDataSourceFactory(val addr: String, val tti: String = "") : DataSource.Factory<String, AccountBlock>() {
    val sourceLiveData = MutableLiveData<TxListPagedDataSource>()
    override fun create(): DataSource<String, AccountBlock> {
        val source = TxListPagedDataSource(addr, tti)
        sourceLiveData.postValue(source)
        return source
    }
}