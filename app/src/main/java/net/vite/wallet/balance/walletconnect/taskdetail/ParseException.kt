package net.vite.wallet.balance.walletconnect.taskdetail


class ParseException private constructor(val code: Int, message: String) : Exception(message) {

    companion object {
        val DummyErrorCode = 1
        val ExtendErrCode = 10000
        val DataErrCode = 10010
        val AmountErrCode = 10020
        val FeeErrCode = 10030
        val TokenIdErrCode = 10040
        fun extendError(message: String = ""): ParseException {
            return ParseException(ExtendErrCode, message)
        }

        fun dataError(message: String = ""): ParseException {
            return ParseException(DataErrCode, message)
        }

        fun amountError(message: String): ParseException {
            return ParseException(AmountErrCode, message)
        }

        fun tokenIdError(message: String): ParseException {
            return ParseException(TokenIdErrCode, message)
        }

        fun feeError(message: String): ParseException {
            return ParseException(FeeErrCode, message)
        }

        fun error(message: String): ParseException {
            return ParseException(DummyErrorCode, message)
        }

    }
}