package net.vite.wallet.balance.crosschain.deposit.list

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.http.gw.DepositRecord


class TxDataSourceFactory(val addr: String, val tti: String, val gwUrl: String) : DataSource.Factory<Int, DepositRecord>() {
    val sourceLiveData = MutableLiveData<TxListPagedDataSource>()
    override fun create(): DataSource<Int, DepositRecord> {
        val source = TxListPagedDataSource(addr, tti, gwUrl)
        sourceLiveData.postValue(source)
        return source
    }
}