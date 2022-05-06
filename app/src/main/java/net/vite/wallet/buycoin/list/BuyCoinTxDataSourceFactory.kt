package net.vite.wallet.buycoin.list

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.http.coinpurchase.PurchaseRecordItem


class BuyCoinTxDataSourceFactory(val addr: String) :
    DataSource.Factory<Int, PurchaseRecordItem>() {
    val sourceLiveData = MutableLiveData<BuyCoinTxListPagedDataSource>()
    override fun create(): DataSource<Int, PurchaseRecordItem> {
        val source = BuyCoinTxListPagedDataSource(addr)
        sourceLiveData.postValue(source)
        return source
    }
}