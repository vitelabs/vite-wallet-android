package net.vite.wallet.widget

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class RxTextWatcher(val predicate: (s: String) -> Boolean) : TextWatcher {
    private val subject = BehaviorSubject.create<String>()

    companion object {
        fun onTextChanged(editText: EditText, predicate: (s: String) -> Boolean = { true }): Observable<String> {
            val watcher = RxTextWatcher(predicate)
            editText.addTextChangedListener(watcher)
            return watcher.subject
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val ns = s?.toString() ?: ""
        if (predicate(ns)) {
            subject.onNext(ns)
        }
    }

}