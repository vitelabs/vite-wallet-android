package net.vite.wallet.balance.quota

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import net.vite.wallet.network.rpc.PledgeInfo


class PledgeDataSourceFactory(val addr: String) : DataSource.Factory<Long, PledgeInfo>() {
    val sourceLiveData = MutableLiveData<PledgeListPagedDataSource>()
    override fun create(): DataSource<Long, PledgeInfo> {
        val source = PledgeListPagedDataSource(addr)
        sourceLiveData.postValue(source)
        return source
    }
}