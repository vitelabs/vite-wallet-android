package net.vite.wallet.balance.ethtxlist

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.eth.EthTransaction


class EthTxDataSourceFactory(val addr: String, val contractAddress: String = "") :
    DataSource.Factory<String, EthTransaction>() {
    val sourceLiveData = MutableLiveData<EthTxListPagedDataSource>()
    override fun create(): DataSource<String, EthTransaction> {
        val source = EthTxListPagedDataSource(addr, contractAddress)
        sourceLiveData.postValue(source)
        return source
    }
}