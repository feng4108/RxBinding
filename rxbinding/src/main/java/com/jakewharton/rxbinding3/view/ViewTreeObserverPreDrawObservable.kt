@file:JvmName("RxView")
@file:JvmMultifileClass

package com.jakewharton.rxbinding3.view

import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import java.util.concurrent.Callable

import com.jakewharton.rxbinding3.internal.checkMainThread

/**
 * Create an observable for pre-draws on `view`.
 *
 * *Warning:* The created observable keeps a strong reference to `view`. Unsubscribe
 * to free this reference.
 *
 * *Warning:* The created observable uses [ViewTreeObserver.addOnPreDrawListener] to
 * observe pre-draws. Multiple observables can be used for a view at a time.
 */
@CheckResult
fun View.preDraws(proceedDrawingPass: Callable<Boolean>): Observable<Unit> {
  return ViewTreeObserverPreDrawObservable(this, proceedDrawingPass)
}

private class ViewTreeObserverPreDrawObservable(
  private val view: View,
  private val proceedDrawingPass: Callable<Boolean>
) : Observable<Unit>() {

  override fun subscribeActual(observer: Observer<in Unit>) {
    if (!checkMainThread(observer)) {
      return
    }
    val listener = Listener(view, proceedDrawingPass, observer)
    observer.onSubscribe(listener)
    view.viewTreeObserver
        .addOnPreDrawListener(listener)
  }

  private class Listener(
    private val view: View,
    private val proceedDrawingPass: Callable<Boolean>,
    private val observer: Observer<in Unit>
  ) : MainThreadDisposable(), OnPreDrawListener {

    override fun onPreDraw(): Boolean {
      if (!isDisposed) {
        observer.onNext(Unit)
        try {
          return proceedDrawingPass.call()
        } catch (e: Exception) {
          observer.onError(e)
          dispose()
        }

      }
      return true
    }

    override fun onDispose() {
      view.viewTreeObserver.removeOnPreDrawListener(this)
    }
  }
}
