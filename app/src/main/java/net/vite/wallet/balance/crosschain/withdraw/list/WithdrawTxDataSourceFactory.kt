package net.vite.wallet.balance.crosschain.withdraw.list

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.http.gw.WithdrawRecord


class WithdrawTxDataSourceFactory(val addr: String, val tti: String, val gwUrl: String) :
    DataSource.Factory<Int, WithdrawRecord>() {
    val sourceLiveData = MutableLiveData<WithdrawTxListPagedDataSource>()
    override fun create(): DataSource<Int, WithdrawRecord> {
        val source = WithdrawTxListPagedDataSource(addr, tti ,gwUrl)
        sourceLiveData.postValue(source)
        return source
    }
}