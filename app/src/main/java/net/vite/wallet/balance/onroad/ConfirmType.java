package net.vite.wallet.balance.onroad;


public enum ConfirmType {
    OK("ok", "确认"),
    NO_TX("tx is not exists", "交易id不存在"),
    TX_IS_NOT_CONFIRM("tx is not confirm", "交易还没有被确认"),
    NO_UNSPEND("not change to unspend", "还没有变为可花费"),
    NO_OUTPUTS("no related outputs", "关联的输出文件不存在"),
    OUTPUT_NOT_ON_CHAIN("output is not exists on chain", "链上输出文件不存在"),
    CONFORM_IS_NOT_ENOUGH("confirm is not enough", "需要足够多的确认");


    public String enname;
    public String cnname;

    ConfirmType(String name, String cnname) {
        this.enname = name;
        this.cnname = cnname;
    }

}